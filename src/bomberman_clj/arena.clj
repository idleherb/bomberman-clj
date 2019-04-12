(ns bomberman-clj.arena
  (:refer-clojure :exclude [eval])
  (:require [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.players :as players]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]))

(defn init
  "Initialize a new (width x height) arena with given players placed"
  [width height players]
  {:post [(specs/valid? ::specs/arena %)]}
  (loop [grid (grid/init width height)
         players players
         player-idx 1]
    (if (> player-idx (count players))
      {:grid grid
       :players (into {} (map (fn [[player-id player]] 
                                  [player-id (:coords player)])
                          players))}
      (let [player-id (keyword (str "player-" player-idx))
            player (players/init (player-id players))
            player (assoc player :player-id player-id)
            coords (if (contains? player :coords)
                     (:coords player)
                     (grid/find-empty-cell grid))]
          (let [grid (grid/assoc-grid-cell grid coords :player
                       (dissoc player :coords))
                players (assoc players player-id
                          (merge player {:coords coords}))
                player-idx (inc player-idx)]
            (recur grid players player-idx))))))

(defn- pickup-item
  [player item]
  (condp = (:type item)
    :bomb (assoc player :bomb-count (inc (:bomb-count player)))
    :fire (assoc player :bomb-radius (inc (:bomb-radius player)))
    (do
      (println "W arena::pickup-item - unknown item:" item)
      player)))

(defn- update-item
  [arena coords]
  (let [{{v :v, :as grid} :grid, players :players} arena
        cell (grid/cell-at grid coords)
        player (grid/cell-player cell)
        item (grid/cell-item cell)]
    (if (and (some? player) (some? item))
      (assoc arena :grid
        (-> grid
            (grid/assoc-grid-cell coords :player
              (pickup-item player item))
            (grid/dissoc-grid-cell coords :item)))
      arena)))

(defn- hit-block
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)
        soft-block (grid/cell-soft-block cell)]
    (if (and (some? soft-block)
          (grid/fire? cell)
          (not (util/hit? soft-block)))
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords :block
          (assoc soft-block :hit {:timestamp timestamp})))
      arena)))

(defn- hit-item
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)
        item (:item cell)]
    (if (and (some? item)
          (grid/fire? cell)
          (not (util/hit? item)))
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords :item
          (assoc item :hit {:timestamp timestamp})))
      arena)))

(defn- hit-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)
        player (grid/cell-player cell)]
    (if (and (some? player) (grid/fire? cell) (not (util/hit? player)))
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords :player
          (assoc player :hit {:timestamp timestamp})))
      arena)))

(defn move
  "Try to move a player in the given direction"
  [arena player-id direction timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  ;  (println "D arena::move" player-id direction)
  (let [{grid :grid, players :players} arena]
    (if-let [coords (player-id players)]
      (let [player (grid/cell-player grid coords)
            new-coords (util/navigate coords direction)]
        (if (and (not (contains? arena :gameover))
              (not (util/hit? player))
              (grid/in-grid? grid new-coords)
              (let [new-cell (grid/cell-at grid new-coords)]
                (or (grid/cell-empty? new-cell)
                  (grid/item? new-cell))))
          (let [grid (-> grid
                         (grid/dissoc-grid-cell coords :player)
                         (grid/assoc-grid-cell new-coords :player player))
                players (assoc players player-id new-coords)
                arena (-> arena
                          (assoc :grid grid
                                 :players players)
                          (hit-player new-coords timestamp)
                          (update-item new-coords))]
            arena)
          arena))
      (do
        (println "D arena::move" player-id "can't move anymore...")
        arena))))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [arena player-id timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  ; (println "D arena::plant-bomb" player-id timestamp)
  (let [{grid :grid, players :players} arena
        coords (player-id players)]
    (if (and (not (contains? arena :gameover)) (some? coords))
      (let [cell (grid/cell-at grid coords)
            player (grid/cell-player cell)]
        (if (and (not (util/hit? player))
              (players/has-bombs? player)
              (not (grid/bomb? cell)))
          (let [bomb {:player-id player-id, :timestamp timestamp}
                player (players/dec-bombs player)
                grid (grid/assoc-grid-cell grid coords :bomb bomb)
                grid (grid/assoc-grid-cell grid coords :player player)]
            (assoc arena :grid grid))
          arena))
      (do
        (println "D arena::plant-bomb" player-id "can't plant bombs anymore...")
        arena))))

(defn- spread-fire
  "Spread fire along x or y axis"
  [{grid :grid, :as arena}
   {x :x, y :y, :as coords}
   transform-coords
   radius
   detonate-bomb
   timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (loop [{{:keys [width height] :as grid} :grid, :as arena} arena
         {cur-x :x, cur-y :y, :as coords} coords
         stop? false]
    (if (or stop?
          (= radius (Math/abs (- cur-x x)))
          (= radius (Math/abs (- cur-y y)))
          (= cur-x -1)
          (= cur-x width)
          (= cur-y -1)
          (= cur-y height))
      arena
      (let [cell (grid/cell-at grid coords)
            arena (-> (if (grid/hard-block? cell)
                        arena
                        (assoc arena :grid
                          (grid/assoc-grid-cell grid coords :fire
                            {:timestamp timestamp})))
                      (detonate-bomb coords timestamp)
                      (hit-block coords timestamp)
                      (hit-item coords timestamp)
                      (hit-player coords timestamp))]
        (recur
          arena
          (transform-coords coords)
          (or (grid/block? cell) (grid/item? cell)))))))

(defn- detonate-bomb
  "Detonate a given bomb"
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, players :players, :as arena} arena
        {x :x, y :y} coords
        bomb (grid/cell-bomb grid coords)]
    (if (and (some? bomb) (not (contains? bomb :detonated)))
      (let [bomb (assoc bomb :detonated {:timestamp timestamp})
            grid (grid/assoc-grid-cell grid coords :bomb bomb)
            player-coords ((:player-id bomb) players)
            player (grid/cell-player grid player-coords)
            player (players/inc-bombs player)
            grid (grid/assoc-grid-cell grid player-coords :player player)
            spread-fire #(spread-fire %1 coords %2 (:bomb-radius player) detonate-bomb timestamp)
            arena (-> (assoc arena :grid grid)
                      (spread-fire (fn [{x :x, y :y}] {:x (inc x), :y y}))
                      (spread-fire (fn [{x :x, y :y}] {:x (dec x), :y y}))
                      (spread-fire (fn [{x :x, y :y}] {:x x, :y (inc y)}))
                      (spread-fire (fn [{x :x, y :y}] {:x x, :y (dec y)})))]
        arena)
    arena)))

(defn- random-item
  []
  (if (< (rand) 1/2)
    {:type :bomb}
    {:type :fire}))

(defn- spawn-random-item
  [arena coords timestamp]
  (if (< (rand) config/chance-spawn-item)
    (assoc arena :grid
      (grid/assoc-grid-cell (:grid arena) coords :item
        (random-item)))
    arena))

(defn- remove-expired-block
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        block (grid/cell-block grid coords)]
    (if (util/block-expired? block timestamp)
      (spawn-random-item
        (assoc arena :grid
          (grid/dissoc-grid-cell grid coords :block))
        coords
        timestamp)
      arena)))

(defn- detonate-timed-out-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [bomb (grid/cell-bomb (:grid arena) coords)]
    (if (util/bomb-timed-out? bomb timestamp)
      (detonate-bomb arena coords timestamp)
      arena)))

(defn- remove-expired-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        bomb (grid/cell-bomb grid coords)]
    (if (util/bomb-expired? bomb timestamp)
      (assoc arena :grid
        (grid/dissoc-grid-cell grid coords :bomb))
      arena)))

(defn- update-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (detonate-timed-out-bomb coords timestamp)
      (remove-expired-bomb coords timestamp)))

(defn- remove-expired-fire
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        fire (grid/cell-fire grid coords)]
    (if (util/fire-expired? fire timestamp)
      (assoc arena :grid (grid/dissoc-grid-cell grid coords :fire))
      arena)))

(defn- remove-expired-item
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        item (grid/cell-item grid coords)]
    (if (util/item-expired? item timestamp)
      (assoc arena :grid (grid/dissoc-grid-cell grid coords :item))
      arena)))

(defn- remove-expired-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, players :players} arena
        player (grid/cell-player grid coords)]
    (if (util/player-expired? player timestamp)
      (assoc arena
        :grid (grid/dissoc-grid-cell grid coords :player)
        :players (assoc players (:player-id player) nil))
      arena)))

(defn- update-gameover
  [arena timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [alive-players (filter (comp some? second) (:players arena))]
    (condp = (count alive-players)
      0 (assoc arena :gameover {:timestamp timestamp})
      1 (assoc arena :gameover {:timestamp timestamp
                                :winner (first (first alive-players))})
      arena)))

(defn eval
  "Check if any bombs should detonate (and detonate in case). Remove expired bombs and fire."
  [arena timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (if (contains? arena :gameover)
    arena
    (let [{{:keys [width height], :as grid} :grid} arena
          arena (loop [arena arena y 0]
                  (if (= height y)
                    arena
                    (recur
                      (loop [arena arena x 0]
                        (if (= width x)
                          arena
                          (recur (-> arena
                                    (remove-expired-block {:x x, :y y} timestamp)
                                    (update-bomb {:x x, :y y} timestamp)
                                    (remove-expired-fire {:x x, :y y} timestamp)
                                    (remove-expired-item {:x x, :y y} timestamp)
                                    (remove-expired-player {:x x, :y y} timestamp))
                                (inc x))))
                        (inc y))))]
          (update-gameover arena timestamp))))
