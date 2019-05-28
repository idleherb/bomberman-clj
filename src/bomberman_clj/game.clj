(ns bomberman-clj.game
  (:refer-clojure :exclude [eval])
  (:require [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.players :as players]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.stats :as stats]
            [bomberman-clj.util :as util]))

(defn init 
  [num-players width height]
  {:post [(specs/valid? ::specs/game %)]}
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

(defn game-over?
  [game]
  (some? (:gameover game)))

(defn join
  [game player timestamp]
  (let [{:keys [num-players players]} game
        cur-num-players (count players)]
    (if (= num-players cur-num-players)
      (do
        (println "E game::join - no free player slot left for player" player)
        game)
      (let [player-id (:player-id player)
            player (assoc player :coords nil)
            players (assoc players player-id player)
            game (assoc game :players players
                             :stats (stats/init-player-stats (:stats game) player-id timestamp))]
        game))))

(defn- active-players
  [game]
  (filter #(not (contains? (second %) :left))
          (:players game)))

(defn- reset
  [game timestamp]
  (let [players (->> (active-players game)
                     (map (fn [[id player]] [id (dissoc player :hit)])))]
    (-> game
        (assoc :players (into {} players)
               :grid nil
               :stats (-> (:stats game)
                          (stats/filter-players (map (fn [[id _]] id) players))
                          (stats/reset-round timestamp)))
        (dissoc :gameover))))

(defn next-round
  [game timestamp]
  {:pre [(specs/valid? ::specs/game game)]
   :post [(specs/valid? ::specs/game %)]}
  (let [{:keys [num-players players width height], :as game} (reset game timestamp)]
    (if (< (count players) num-players)
      (do
        (println "W game::next-round - not enough players to start new round...")
        (assoc game :in-progress? false))
      (loop [grid (grid/init width height)
             players players
             i 1]
        (if (> i (count players))
          (assoc game :grid grid
                      :players players
                      :in-progress? true)
          (let [coords (grid/find-empty-cell grid)
                player-id (keyword (str "player-" i))
                player (-> (get players player-id)
                           (players/init )
                           (assoc :coords coords))
                grid (grid/assoc-grid-cell grid coords :player-id player-id)
                players (assoc players player-id player)]
            (recur grid players (inc i))))))))

(defn cell-player
  [game cell]
  (let [players (:players game)
        player-id (grid/cell-player-id cell)]
    (get players player-id)))

(defn- pickup-item
  [player item]
  (condp = (:type item)
    :bomb (assoc player :bomb-count (inc (:bomb-count player)))
    :fire (assoc player :bomb-radius (inc (:bomb-radius player)))
    (do
      (println "W game::pickup-item - unknown item:" item)
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
        cell (grid/cell-at grid coords)
        player (cell-player game cell)
        item (grid/cell-item cell)]
    (if (and (some? player) (some? item))
      (-> game
          (update-player (pickup-item player item))
          (assoc :grid (grid/dissoc-grid-cell grid coords :item)))
      game)))

(defn- hit-block
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        cell (grid/cell-at grid coords)
        soft-block (grid/cell-soft-block cell)]
    (if (and (some? soft-block)
          (grid/fire? cell)
          (not (util/hit? soft-block)))
      (assoc game :grid
        (grid/assoc-grid-cell grid coords :block
          (assoc soft-block :hit {:timestamp timestamp})))
      game)))

(defn- hit-item
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        cell (grid/cell-at grid coords)
        item (:item cell)]
    (if (and (some? item)
          (grid/fire? cell)
          (not (util/hit? item)))
      (assoc game :grid
        (grid/assoc-grid-cell grid coords :item
          (assoc item :hit {:timestamp timestamp})))
      game)))

(defn- hit-player
  ([game coords timestamp force?]
    ; {:pre [(specs/valid? ::specs/game game)
    ;        (specs/valid? ::specs/coords coords)
    ;        (specs/valid? ::specs/timestamp timestamp)]
    ;  :post [(specs/valid? ::specs/game %)]}
    (let [grid (:grid game)
          cell (grid/cell-at grid coords)
          fire? (grid/fire? cell)
          player (cell-player game cell)]
      (if (and (some? player) (or force? fire?) (not (util/hit? player)))
        (let [player (cond-> (assoc-in player [:hit :timestamp] timestamp)
                       fire? (assoc-in [:hit :player-id] (:player-id (grid/cell-fire cell)))
                       force? (assoc-in [:hit :player-id] (:player-id player)))]
          (update-player game player))
        game)))
  ([game coords timestamp]
    (hit-player game coords timestamp false)))

(defn move
  "Try to move a player in the given direction"
  [game player-id direction timestamp]
  ; {:pre [(specs/valid? ::specs/game game)]
  ;  :post [(specs/valid? ::specs/game %)]}
  ;  (println "D game::move" player-id direction)
  (let [{:keys [grid in-progress? players]} game]
    (if (not in-progress?)
      (do
        (println "W game::move -" player-id "can't move, game not in progress")
        game)
      (if-let [coords (:coords (get players player-id))]  ; TODO: better semantic query (not in-progress or game-over)
        (let [cell (grid/cell-at (:grid game) coords)
              player (cell-player game cell)
              new-coords (util/navigate coords direction)]
          (if (and (not (contains? game :gameover))
                (not (util/hit? player))
                (grid/in-grid? grid new-coords)
                (let [new-cell (grid/cell-at grid new-coords)]
                  (or (grid/cell-empty? new-cell)
                    (grid/item? new-cell))))
            (let [player (assoc player :coords new-coords)
                  grid (-> grid
                          (grid/dissoc-grid-cell coords :player-id)
                          (grid/assoc-grid-cell new-coords :player-id player-id))
                  game (-> game
                           (update-player player)
                           (assoc :grid grid
                                  :stats (stats/inc-player-moves (:stats game) player-id))
                           (hit-player new-coords timestamp)
                           (update-item new-coords))]
              game)
            game))
        (do
          (println "D game::move" player-id "can't move anymore...")
          game)))))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [game player-id timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  ; (println "D game::plant-bomb" player-id timestamp)
  (let [{:keys [grid in-progress? players]} game
        {:keys [coords], :as player} (get players player-id)]
    (if (not in-progress?)
      (do
        (println "W game::plant-bomb -" player-id "can't plant bombs, game not in progress")
        game)
      (if (and (not (contains? game :gameover)) (some? coords))
        (if (and (not (util/hit? player))
              (players/has-bombs? player)
              (not (grid/bomb? grid coords)))
          (let [bomb {:player-id player-id, :timestamp timestamp}
                grid (grid/assoc-grid-cell grid coords :bomb bomb)
                player (players/dec-bombs player)]
            (-> game
                (update-player player)
                (assoc :grid grid)))
          game)
        (do
          (println "D game::plant-bomb" player-id "can't plant bombs anymore...")
          game)))))

(defn leave
  [game player-id timestamp]
  {:pre [(specs/valid? ::specs/game game)
         (specs/valid? ::specs/player-id player-id)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/game %)]}
  (println "D game::leave" player-id timestamp)
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
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
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
      (let [cell (grid/cell-at grid coords)
            game (-> (if (grid/hard-block? cell)
                        game
                        (assoc game :grid
                          (grid/assoc-grid-cell grid coords :fire
                            {:player-id player-id
                             :timestamp timestamp})))
                      (detonate-bomb coords timestamp)
                      (hit-block coords timestamp)
                      (hit-item coords timestamp)
                      (hit-player coords timestamp))]
        (recur
          game
          (transform-coords coords)
          (or (grid/block? cell) (grid/item? cell)))))))

(defn- detonate-bomb
  "Detonate a given bomb"
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [{grid :grid, players :players, :as game} game
        bomb (grid/cell-bomb grid coords)]
    (if (and (some? bomb) (not (contains? bomb :detonated)))
      (let [bomb (assoc bomb :detonated {:timestamp timestamp})
            grid (grid/assoc-grid-cell grid coords :bomb bomb)
            player-id (:player-id bomb)
            player (get players player-id)
            player-coords (:coords player)
            player (players/inc-bombs player)
            grid (grid/assoc-grid-cell grid player-coords :player-id player-id)
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

(defn- random-item
  []
  (if (< (rand) 1/2)
    {:type :bomb}
    {:type :fire}))

(defn- spawn-random-item
  [game coords timestamp]
  (if (< (rand) config/chance-spawn-item)
    (assoc game :grid
      (grid/assoc-grid-cell (:grid game) coords :item
        (random-item)))
    game))

(defn- remove-expired-block
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        block (grid/cell-block grid coords)]
    (if (util/block-expired? block timestamp)
      (spawn-random-item
        (assoc game :grid
          (grid/dissoc-grid-cell grid coords :block))
        coords
        timestamp)
      game)))

(defn- detonate-timed-out-bomb
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [bomb (grid/cell-bomb (:grid game) coords)]
    (if (util/bomb-timed-out? bomb timestamp)
      (detonate-bomb game coords timestamp)
      game)))

(defn- remove-expired-bomb
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        bomb (grid/cell-bomb grid coords)]
    (if (util/bomb-expired? bomb timestamp)
      (assoc game :grid
        (grid/dissoc-grid-cell grid coords :bomb))
      game)))

(defn- update-bomb
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (-> game
      (detonate-timed-out-bomb coords timestamp)
      (remove-expired-bomb coords timestamp)))

(defn- remove-expired-fire
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        fire (grid/cell-fire grid coords)]
    (if (util/fire-expired? fire timestamp)
      (assoc game :grid (grid/dissoc-grid-cell grid coords :fire))
      game)))

(defn- remove-expired-item
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        item (grid/cell-item grid coords)]
    (if (util/item-expired? item timestamp)
      (assoc game :grid (grid/dissoc-grid-cell grid coords :item))
      game)))

(defn- remove-expired-player
  [game coords timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [grid (:grid game)
        cell (grid/cell-at grid coords)
        player (cell-player game cell)]
    (if (util/player-expired? player timestamp)
      (let [grid (grid/dissoc-grid-cell grid coords :player-id)
            player (assoc player :coords nil)]
        (-> game
            (update-player player)
            (assoc :grid grid)))
      game)))

(defn- update-gameover
  [game timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
  (let [alive-players (filter (comp some? :coords second) (:players game))]
    (condp = (count alive-players)
      0 (assoc game :gameover {:timestamp timestamp})
      1 (assoc game :gameover {:timestamp timestamp
                               :winner (first (first alive-players))})
      game)))

(defn- update-stats-from-coords
  [game coords timestamp]
  (if-let [player (cell-player game (grid/cell-at (:grid game) coords))]
    (if-let [hit (:hit player)]
      (let [stats (:stats game)
            corpse-id (:player-id player)
            killer-id (:player-id hit)]
        (if (= corpse-id killer-id)
          (assoc game :stats (stats/add-suicide stats corpse-id))
          (assoc game :stats (stats/add-kill stats killer-id corpse-id))))
      game)
    game))

(defn- update-stats
  [game timestamp]
  (let [winner (get-in game [:gameover :winner])
        stats (cond-> (:stats game)
                true (stats/update-time timestamp)
                (some? winner) (update-in [:all :players winner :wins] inc))]
    (assoc game :stats stats)))

(defn eval
  "Check if any bombs should detonate (and detonate in case). Remove expired bombs and fire."
  [game timestamp]
  ; {:pre [(specs/valid? ::specs/game game)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/game %)]}
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
