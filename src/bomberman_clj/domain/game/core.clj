(ns bomberman-clj.domain.game.core
  (:refer-clojure :exclude [eval])
  (:require [bomberman-clj.config :as c]
            [bomberman-clj.domain.game.grid :as g]
            [bomberman-clj.domain.game.players :as p]
            [bomberman-clj.domain.game.specs :as sp]
            [bomberman-clj.domain.game.stats :as st]
            [bomberman-clj.domain.game.util :as u]))

(defn init 
  [num-players width height]
  {:post [(sp/valid? ::sp/game %)]}
  {:num-players num-players
   :players {}
   :stats {:round {:started-at nil
                   :duration 0
                   :players {}}
           :all {:players {}}}
   :grid nil
   :in-progress? false
   :width width
   :height height})

(defn gameover-expired?
  [game timestamp]
  (u/gameover-expired? (:gameover game) timestamp))

(defn join
  [game player timestamp]
  (let [{:keys [num-players players]} game
        cur-num-players (count players)]
    (if (= num-players cur-num-players)
      (do
        (println "E d.g.core::join - no free player slot left for player" player)
        game)
      (let [player-id (:player-id player)
            player (assoc player :coords nil)
            players (assoc players player-id player)
            game (assoc game :players players
                             :stats (st/init-player-stats (:stats game) player-id timestamp))]
        game))))

(defn- active-players
  [game]
  (filter #(not (contains? (second %) :left))
          (:players game)))

(defn- reset
  [game timestamp]
  (let [players (->> (active-players game)
                     (map (fn [[id player]] [id (dissoc player :hit
                                                               :bomb-kick?
                                                               :remote-control?)])))]
    (-> game
        (assoc :players (into {} players)
               :grid nil
               :stats (-> (:stats game)
                          (st/filter-players (map (fn [[id _]] id) players))
                          (st/reset-round timestamp)))
        (dissoc :gameover))))

(defn next-round
  [game timestamp]
  (let [{:keys [num-players players width height], :as game} (reset game timestamp)]
    (if (< (count players) num-players)
      (do
        (println "W d.g.core::next-round - not enough players to start new round...")
        (assoc game :in-progress? false))
      (loop [grid (g/init width height)
             players players
             i 1]
        (if (> i (count players))
          (assoc game :grid grid
                      :players players
                      :in-progress? true)
          (let [player-id (keyword (str "player-" i))
                player (-> (get players player-id)
                           (p/init))
                [grid player] (g/spawn-player grid player)
                players (assoc players player-id player)]
            (recur grid players (inc i))))))))

(defn cell-player
  [game cell]
  (let [players (:players game)
        player-id (g/cell-player-id cell)]
    (get players player-id)))

(defn- pickup-item
  [player item]
  (condp = (:type item)
    :bomb (assoc player :bomb-count (inc (:bomb-count player)))
    :fire (assoc player :bomb-radius (inc (:bomb-radius player)))
    :bomb-kick (assoc player :bomb-kick? true)
    :remote-control (assoc player :remote-control? true)
    (do
      (println "W d.g.core::pickup-item - unknown item:" item)
      player)))

(defn- update-player
  [game player]
  (let [players (:players game)
        player-id (:player-id player)]
    (assoc game :players
      (assoc players player-id player))))

(defn- update-item
  [game coords]
  (let [grid (:grid game)
        cell (g/cell-at grid coords)
        player (cell-player game cell)
        item (g/cell-item cell)]
    (if (and (some? player) (some? item))
      (-> game
          (update-player (pickup-item player item))
          (assoc :grid (g/dissoc-grid-cell grid coords :item)))
      game)))

(defn- hit-block
  [game coords timestamp]
  (let [grid (:grid game)
        cell (g/cell-at grid coords)
        soft-block (g/cell-soft-block cell)]
    (if (and (some? soft-block)
          (g/fire? cell)
          (not (u/hit? soft-block)))
      (assoc game :grid
        (g/assoc-grid-cell grid coords :block
          (assoc soft-block :hit {:timestamp timestamp})))
      game)))

(defn- hit-item
  [game coords timestamp]
  (let [grid (:grid game)
        cell (g/cell-at grid coords)
        item (:item cell)]
    (if (and (some? item)
          (g/fire? cell)
          (not (u/hit? item)))
      (assoc game :grid
        (g/assoc-grid-cell grid coords :item
          (assoc item :hit {:timestamp timestamp})))
      game)))

(defn- hit-player
  ([game coords timestamp force?]
    (let [grid (:grid game)
          cell (g/cell-at grid coords)
          fire? (g/fire? cell)
          player (cell-player game cell)]
      (if (and (some? player) (or force? fire?) (not (u/hit? player)))
        (let [player (cond-> (assoc-in player [:hit :timestamp] timestamp)
                       fire? (assoc-in [:hit :player-id] (:player-id (g/cell-fire cell)))
                       force? (assoc-in [:hit :player-id] (:player-id player)))]
          (update-player game player))
        game)))
  ([game coords timestamp]
    (hit-player game coords timestamp false)))

(defn move
  "Try to move a player in the given direction"
  [game player-id direction timestamp]
  (let [{:keys [grid in-progress? players]} game]
    (if (not in-progress?)
      (do
        (println "W d.g.core::move -" player-id "can't move, game not in progress")
        game)
      (if-let [coords (:coords (get players player-id))]  ; TODO: better semantic query (not in-progress or gameover)
        (let [cell (g/cell-at grid coords)
              player (cell-player game cell)
              new-coords (g/navigate coords grid direction)]
          (if (and (not= new-coords coords)
                   (not (contains? game :gameover))
                   (not (u/hit? player))
                   (let [new-cell (g/cell-at grid new-coords)]
                     (or (g/cell-empty? new-cell)
                         (g/item? new-cell))))
            (let [player (assoc player :coords new-coords)
                  grid (-> grid
                           (g/dissoc-grid-cell coords :player-id)
                           (g/assoc-grid-cell new-coords :player-id player-id))
                  game (-> game
                           (update-player player)
                           (assoc :grid grid
                                  :stats (st/add-move (:stats game) player-id))
                           (hit-player new-coords timestamp)
                           (update-item new-coords))]
              game)
            (let [new-cell (g/cell-at grid new-coords)]
              (if (and (:bomb-kick? player) (g/bomb? new-cell))
                (let [bomb (g/cell-bomb new-cell)
                      kicked-bomb (assoc bomb :kick {:direction direction
                                                     :timestamp (- timestamp c/bomb-kick-speed-ms)})]
                  (update game :grid g/assoc-grid-cell new-coords :bomb kicked-bomb))
                game))))
        (do
          (println "D d.g.core::move" player-id "can't move anymore...")
          game)))))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [game player-id timestamp]
  (let [{:keys [grid in-progress? players]} game
        {:keys [coords], :as player} (get players player-id)]
    (if (not in-progress?)
      (do
        (println "W d.g.core::plant-bomb -" player-id "can't plant bombs, game not in progress")
        game)
      (if (and (not (contains? game :gameover)) (some? coords))
        (if (and (not (u/hit? player))
              (p/has-bombs? player)
              (not (g/bomb? grid coords)))
          (let [bomb {:player-id player-id, :timestamp timestamp}
                grid (g/assoc-grid-cell grid coords :bomb bomb)
                player (p/dec-bombs player)]
            (-> game
                (update-player player)
                (assoc :grid grid)))
          game)
        (do
          (println "D d.g.core::plant-bomb" player-id "can't plant bombs anymore...")
          game)))))

(defn leave
  [game player-id timestamp]
  (println "D d.g.core::leave -" player-id "left the game at" timestamp)
  (let [{:keys [in-progress? players]} game]
    (if in-progress?
      (let [{:keys [coords left], :as player} (get players player-id)
            player (if (nil? left)
                     (assoc player :left {:timestamp timestamp})
                     player)]
        (-> game
            (update-player player)
            (hit-player coords timestamp true)))
      (assoc game :players (dissoc players player-id)))))

(defn- spread-fire
  "Spread fire along x or y axis"
  [game
   {x :x, y :y, :as coords}
   transform-coords
   radius
   detonate-bomb
   player-id
   timestamp]
  (loop [{{:keys [width height] :as grid} :grid, :as game} game
         {cur-x :x, cur-y :y, :as coords} coords
         stop? false]
    (if (or stop?
          (= radius (Math/abs (- cur-x x)))
          (= radius (Math/abs (- cur-y y)))
          (= cur-x -1)
          (= cur-x width)
          (= cur-y -1)
          (= cur-y height))
      game
      (let [cell (g/cell-at grid coords)
            game (-> (if (g/hard-block? cell)
                        game
                        (assoc game :grid
                          (g/assoc-grid-cell grid coords :fire
                            {:player-id player-id
                             :timestamp timestamp})))
                      (detonate-bomb coords timestamp)
                      (hit-block coords timestamp)
                      (hit-item coords timestamp)
                      (hit-player coords timestamp))]
        (recur
          game
          (transform-coords coords)
          (or (g/block? cell) (g/item? cell)))))))

(defn- detonate-bomb
  "Detonate a given bomb"
  [game coords timestamp]
  (let [{grid :grid, players :players, :as game} game
        bomb (g/cell-bomb grid coords)]
    (if (and (some? bomb) (not (contains? bomb :detonated)))
      (let [bomb (assoc bomb :detonated {:timestamp timestamp})
            grid (g/assoc-grid-cell grid coords :bomb bomb)
            player-id (:player-id bomb)
            player (get players player-id)
            player-coords (:coords player)
            player (p/inc-bombs player)
            grid (g/assoc-grid-cell grid player-coords :player-id player-id)
            spread-fire #(spread-fire %1 coords %2 (:bomb-radius player) detonate-bomb player-id timestamp)
            game (-> game
                     (update-player player)
                     (assoc :grid grid)
                     (spread-fire (fn [{x :x, y :y}] {:x (inc x), :y y}))
                     (spread-fire (fn [{x :x, y :y}] {:x (dec x), :y y}))
                     (spread-fire (fn [{x :x, y :y}] {:x x, :y (inc y)}))
                     (spread-fire (fn [{x :x, y :y}] {:x x, :y (dec y)})))]
        game)
    game)))

(defn remote-detonate-bombs
  [game player-id timestamp]
  (let [{:keys [in-progress? gameover players]} game
        {:keys [remote-control?]} (player-id players)]
    (if (or (not in-progress?) (some? gameover) (not remote-control?))
      game
      (let [{{:keys [width height]} :grid} game]
        (loop [game game y 0]
          (if (= height y)
            game
            (recur
             (loop [game game x 0]
               (if (= width x)
                 game
                 (recur (let [coords {:x x, :y y}
                              bomb (g/cell-bomb (:grid game) coords)]
                          (if (and (some? bomb) (= (:player-id bomb) player-id))
                            (detonate-bomb game {:x x, :y y} timestamp)
                            game))
                        (inc x))))
             (inc y))))))))

(defn- random-rare-item
  []
  (if (< (rand) 1/2)
    {:type :bomb-kick}
    {:type :remote-control}))

(defn- random-item
  []
  (if (< (rand) 1/2)
    {:type :bomb}
    {:type :fire}))

(defn- spawn-random-item
  [game coords timestamp]
  (if-let [item (condp > (rand)
                  c/chance-spawn-rare-item (random-rare-item)
                  c/chance-spawn-item (random-item)
                  nil)]
    (assoc game :grid (g/assoc-grid-cell (:grid game) coords :item item))
    game))

(defn- remove-expired-block
  [game coords timestamp]
  (let [grid (:grid game)
        block (g/cell-block grid coords)]
    (if (u/block-expired? block timestamp)
      (spawn-random-item
        (assoc game :grid
          (g/dissoc-grid-cell grid coords :block))
        coords
        timestamp)
      game)))

(defn- move-kicked-bomb
  [game coords timestamp]
  (let [grid (:grid game)
        bomb (g/cell-bomb grid coords)
        kicked (:kick bomb)]
    (if (and (some? kicked)
             (u/expired? (:timestamp kicked) timestamp c/bomb-kick-speed-ms))
      (let [new-coords (g/navigate coords grid (:direction kicked))]
        (if (g/cell-empty? grid new-coords)
          (let [new-bomb (assoc-in bomb [:kick :timestamp] timestamp)
                game (assoc game :grid (g/dissoc-grid-cell grid coords :bomb))
                game (assoc game :grid (g/assoc-grid-cell (:grid game) new-coords :bomb new-bomb))]
            game)
          (assoc game :grid (g/assoc-grid-cell grid coords :bomb (dissoc bomb :kick)))))
      game)))

(defn- detonate-timed-out-bomb
  [game coords timestamp]
  (let [bomb (g/cell-bomb (:grid game) coords)
        {:keys [remote-control?]} (get-in game [:players (:player-id bomb)])]
    (if (and (u/bomb-timed-out? bomb timestamp)
             (not remote-control?))
      (detonate-bomb game coords timestamp)
      game)))

(defn- remove-expired-bomb
  [game coords timestamp]
  (let [grid (:grid game)
        bomb (g/cell-bomb grid coords)]
    (if (u/bomb-expired? bomb timestamp)
      (assoc game :grid
        (g/dissoc-grid-cell grid coords :bomb))
      game)))

(defn- update-bomb
  [game coords timestamp]
  (-> game
      (move-kicked-bomb coords timestamp)
      (detonate-timed-out-bomb coords timestamp)
      (remove-expired-bomb coords timestamp)))

(defn- remove-expired-fire
  [game coords timestamp]
  (let [grid (:grid game)
        fire (g/cell-fire grid coords)]
    (if (u/fire-expired? fire timestamp)
      (assoc game :grid (g/dissoc-grid-cell grid coords :fire))
      game)))

(defn- remove-expired-item
  [game coords timestamp]
  (let [grid (:grid game)
        item (g/cell-item grid coords)]
    (if (u/item-expired? item timestamp)
      (assoc game :grid (g/dissoc-grid-cell grid coords :item))
      game)))

(defn- remove-expired-player
  [game coords timestamp]
  (let [grid (:grid game)
        cell (g/cell-at grid coords)
        player (cell-player game cell)]
    (if (u/player-expired? player timestamp)
      (let [grid (g/dissoc-grid-cell grid coords :player-id)
            player (assoc player :coords nil)]
        (-> game
            (update-player player)
            (assoc :grid grid)))
      game)))

(defn- update-gameover
  [game timestamp]
  (let [alive-players (filter (comp some? :coords second) (:players game))]
    (condp = (count alive-players)
      0 (assoc game :gameover {:timestamp timestamp})
      1 (assoc game :gameover {:timestamp timestamp
                               :winner (first (first alive-players))})
      game)))

(defn- update-stats-from-coords
  [game coords timestamp]
  (if-let [player (cell-player game (g/cell-at (:grid game) coords))]
    (let [hit (:hit player)
          stats-collected (:stats-collected hit)]
      (if (and (some? hit) (nil? stats-collected))
        (let [corpse-id (:player-id player)
              killer-id (:player-id hit)
              stats (:stats game)
              player (assoc-in player [:hit :stats-collected] timestamp)
              game (assoc-in game [:players corpse-id] player)]
          (if (= corpse-id killer-id)
            (assoc game :stats (st/add-suicide stats corpse-id))
            (assoc game :stats (st/add-kill stats killer-id corpse-id))))
        game))
      game))

(defn- update-stats
  [game timestamp]
  (let [duration (- timestamp (get-in game [:stats :round :started-at]))
        winner (get-in game [:gameover :winner])]
    (cond-> game
      true (assoc-in [:stats :round :duration] duration)
      (some? winner) (update-in [:stats] st/add-win winner))))

(defn eval
  "Check if any bombs should detonate (and detonate in case). Remove expired bombs and fire."
  [game timestamp]
  (if (or (not (:in-progress? game)) (contains? game :gameover))
    game
    (let [{{:keys [width height]} :grid} game
          game (loop [game game y 0]
                 (if (= height y)
                   game
                   (recur
                    (loop [game game x 0]
                      (if (= width x)
                        game
                        (recur (-> game
                                   (remove-expired-block {:x x, :y y} timestamp)
                                   (update-bomb {:x x, :y y} timestamp)
                                   (remove-expired-fire {:x x, :y y} timestamp)
                                   (remove-expired-item {:x x, :y y} timestamp)
                                   (remove-expired-player {:x x, :y y} timestamp)
                                   (update-stats-from-coords {:x x, :y y} timestamp))
                               (inc x))))
                    (inc y))))]
      (-> game
          (update-gameover timestamp)
          (update-stats timestamp)))))
