(ns bomberman-clj.arena
  (:refer-clojure :exclude [eval])
  (:require [bomberman-clj.bombs :as bombs]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.players :as players]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]))

(defn player-by-id
  [arena player-id]
  (let [coords (player-id (:players arena))
        grid (:grid arena)
        player (grid/player-at grid coords)]
    player))

(defn init
  "Initialize a new (width x height) arena with given players placed"
  [width height players]
  {:post [(specs/valid? ::specs/arena %)]}
  (loop [grid (grid/init width height)
         players players
         player-idx 1]
    (if (> player-idx (count players))
      {:bombs {}
       :grid grid
       :players (into {} (map (fn [[player-id player]] 
                                  [player-id (:coords player)])
                          players))}
      (let [player-id (keyword (str "player-" player-idx))
            player (players/init (player-id players))]
        (if (contains? player :coords)
          ; spawn player at given coords
          (let [grid (grid/spawn grid
                                 player-id
                                 (dissoc player :coords)
                                 (:coords player))
                players (assoc players player-id player)
                player-idx (inc player-idx)]
            (recur grid players player-idx))
          ; spawn player at random coords
          (let [coords (grid/find-empty-cell grid)
                grid (grid/spawn grid player-id player coords)
                players (assoc players player-id (merge player {:coords coords}))
                player-idx (inc player-idx)]
            (recur grid players player-idx)))))))

(defn- pickup-item
  [player item]
  (condp = (:type item)
    :bomb (assoc player :bomb-count (inc (:bomb-count player)))
    (do
      (println "W arena::pickup-item - unknown item:" item)
      player)))

(defn- update-item
  [arena coords]
  (let [{{v :v, :as grid} :grid, players :players} arena
        player (grid/player-at grid coords)
        item (grid/item-at grid coords)]
    (if (and (some? player) (some? item))
      (assoc arena :grid
        (-> grid
            (grid/assoc-grid-cell ,,, coords (grid/player-id-at grid coords)
              (pickup-item player item))
            (grid/dissoc-grid-cell ,,, coords :item)))
      arena)))

(defn move
  "Try to move a player in the given direction"
  [arena player-id direction]
  ; {:pre [(specs/valid? ::specs/arena arena)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  ;  (println "D arena::move" player-id direction)
  (let [{{v :v, :as grid} :grid, players :players} arena]
    (if-let [coords (player-id players)]
      (let [player (player-id (grid/cell-at grid coords))
            new-coords (util/navigate coords direction)]
        (if (and (not (contains? arena :gameover))
                 (not (contains? player :hit))
                 (grid/in-grid? grid new-coords)
                 (or (grid/cell-empty? grid new-coords)
                     (grid/item? grid new-coords)))
          (let [grid (grid/dissoc-grid-cell grid coords player-id)
                grid (grid/spawn grid player-id player new-coords)
                players (assoc players player-id new-coords)
                arena (assoc arena :grid grid
                                   :players players)
                arena (update-item arena new-coords)]
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
  (let [{{v :v, :as grid} :grid, players :players, bombs :bombs} arena]
    (if-let [{x :x, y :y, :as coords} (player-id players)]
      (let [player (grid/player-at grid player-id coords)]
        (if (and (not (contains? arena :gameover))
                 (not (contains? player :hit))
                 (players/has-bombs? player)
                 (not (grid/bomb? grid coords)))
          (let [bomb-id (keyword (str "bomb-x" x "y" y))
                bomb {:player-id player-id, :timestamp timestamp}
                bombs (assoc bombs bomb-id coords)
                player (players/dec-bombs player)
                grid (grid/assoc-grid-cell grid coords bomb-id bomb)
                grid (grid/assoc-grid-cell grid coords player-id player)]
              (assoc arena :bombs bombs
                          :grid grid))
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
  (loop [{cur-x :x, cur-y :y, :as coords} coords
         {:keys [width height], :as grid} grid
         stop false]
    (if (or stop
            (= radius (Math/abs (- cur-x x)))
            (= radius (Math/abs (- cur-y y)))
            (= cur-x -1)
            (= cur-x width)
            (= cur-y -1)
            (= cur-y height))
      (assoc arena :grid grid)
      (let [bomb-id (keyword (str "bomb-x" cur-x "y" cur-y))
            bomb (grid/bomb-at grid bomb-id coords)
            arena (assoc arena :grid grid)
            arena (if (and (not (nil? bomb))
                           (not (contains? bomb :detonated)))
              (detonate-bomb arena bomb-id timestamp)
              arena)
            grid (:grid arena)
            hard-block? (grid/hard-block? grid coords)
            soft-block? (grid/soft-block? grid coords)
            block? (or hard-block? soft-block?)]
        (recur
          (transform-coords coords)
          (if hard-block?
            grid
            (grid/assoc-grid-cell grid coords :fire {:timestamp timestamp}))
          block?)))))

(defn- detonate-bomb
  "Detonate a given bomb"
  [arena bomb-id timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, bombs :bombs, :as arena} arena
        {x :x, y :y, :as coords} (bomb-id bombs)
        bomb (assoc (grid/bomb-at grid bomb-id coords) :detonated {:timestamp timestamp})
        grid (grid/assoc-grid-cell grid coords bomb-id bomb)
        player-id (:player-id bomb)
        player-coords (player-id (:players arena))
        player (grid/player-at grid player-id player-coords)
        player (players/inc-bombs player)
        grid (grid/assoc-grid-cell grid player-coords player-id player)
        arena (assoc arena :grid grid)
        spread-fire #(spread-fire %1 coords %2 (:bomb-radius player) detonate-bomb timestamp)
        arena (spread-fire arena (fn [{x :x, y :y}] {:x (inc x), :y y}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x (dec x), :y y}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x x, :y (inc y)}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x x, :y (dec y)}))]
    arena))

(defn- hit-block
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)
        block (:block cell)]
    (if (and (some? block)
             (grid/soft-block? grid coords)
             (grid/fire? grid coords)
             (nil? (:hit block)))
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords :block
          (assoc block :hit {:timestamp timestamp})))
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
        cell (grid/cell-at grid coords)
        block (:block cell)
        hit (:hit block)]
    (if (and (some? block)
             (some? (:hit block))
             (<= config/block-expiration-ms (- timestamp (:timestamp (:hit block)))))
      (spawn-random-item
        (assoc arena :grid
          (grid/dissoc-grid-cell grid coords :block))
        coords
        timestamp)
      arena)))

(defn- update-block
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (hit-block coords timestamp)
      (remove-expired-block coords timestamp)))

(defn- remove-expired-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [bomb-id (cells/cell-bomb-id cell)]
      (assoc arena :grid
        (if (bombs/bomb-expired? (bomb-id cell) timestamp)
          (grid/dissoc-grid-cell grid coords bomb-id)
          grid))
      arena)))

(defn- detonate-timed-out-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [bomb-id (cells/cell-bomb-id cell)]
      (let [bomb (bomb-id cell)]
        (if (and (not (contains? bomb :detonated))
                 (<= config/bomb-timeout-ms (- timestamp (:timestamp bomb))))
          (detonate-bomb arena bomb-id timestamp)
          arena))
      arena)))

(defn- update-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (remove-expired-bomb coords timestamp)
      (detonate-timed-out-bomb coords timestamp)))

(defn- hit-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [player-id (cells/cell-player-id cell)]
      (let [player (player-id cell)]
        (if (and (grid/fire? grid coords)
                 (nil? (:hit player)))
          (assoc arena :grid
            (grid/assoc-grid-cell grid coords player-id
              (assoc (grid/player-at grid player-id coords) :hit {:timestamp timestamp})))
          arena))
      arena)))

(defn- remove-expired-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [player-id (cells/cell-player-id cell)]
      (if-let [hit (:hit (player-id cell))]
        (assoc arena
          :grid (if (<= config/player-expiration-ms (- timestamp (:timestamp hit)))
            (grid/dissoc-grid-cell grid coords player-id)
            grid)
          :players (assoc (:players arena) player-id nil))
        arena)
      arena)))

(defn- update-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (hit-player coords timestamp)
      (remove-expired-player coords timestamp)))

(defn- update-fire
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;         (specs/valid? ::specs/coords coords)
  ;         (specs/valid? ::specs/timestamp timestamp)]
  ;   :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [fire (:fire cell)]
      (assoc arena :grid
        (if (<= config/fire-expiration-ms (- timestamp (:timestamp fire)))
          (grid/dissoc-grid-cell grid coords :fire)
          grid))
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
                                    (update-block  {:x x, :y y} timestamp)
                                    (update-bomb   {:x x, :y y} timestamp)
                                    (update-player {:x x, :y y} timestamp)
                                    (update-fire   {:x x, :y y} timestamp))
                                (inc x))))
                        (inc y))))]
          (update-gameover arena timestamp))))
