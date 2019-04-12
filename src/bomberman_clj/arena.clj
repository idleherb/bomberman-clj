(ns bomberman-clj.arena
  (:refer-clojure :exclude [eval])
  (:require [bomberman-clj.bombs :as bombs]
            [bomberman-clj.config :as config]
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
            (grid/assoc-grid-cell ,,, coords :player
              (pickup-item player item))
            (grid/dissoc-grid-cell ,,, coords :item)))
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
    (if (and (some? player) (grid/fire? cell) (nil? (:hit player)))
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
              (not (contains? player :hit))
              (grid/in-grid? grid new-coords)
              (let [new-cell (grid/cell-at grid new-coords)]
                (or (grid/cell-empty? new-cell)
                  (grid/item? new-cell))))
          (let [grid (grid/dissoc-grid-cell grid coords :player)
                grid (grid/assoc-grid-cell grid new-coords :player player)
                players (assoc players player-id new-coords)
                arena (assoc arena :grid grid
                                   :players players)
                arena (hit-player arena new-coords timestamp)
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
  (let [{grid :grid, players :players} arena
        coords (player-id players)]
    (if (and (not (contains? arena :gameover)) (some? coords))
      (let [cell (grid/cell-at grid coords)
            player (grid/cell-player cell)]
        (if (and (not (contains? player :hit))
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
      (let [cell (grid/cell-at grid coords)
            bomb (grid/cell-bomb cell)
            arena (assoc arena :grid grid)
            arena (if (and (some? bomb)
                        (not (contains? bomb :detonated)))
              (detonate-bomb arena coords timestamp)
              arena)
            grid (:grid arena)
            cell (grid/cell-at grid coords)
            empty? (grid/cell-empty? cell)
            hard-block? (grid/hard-block? cell)
            soft-block? (grid/soft-block? cell)
            item? (grid/item? cell)]
        (recur
          (transform-coords coords)
          (if hard-block?
            grid
            (grid/assoc-grid-cell grid coords :fire {:timestamp timestamp}))
          (or hard-block? soft-block? item?))))))

(defn- detonate-bomb
  "Detonate a given bomb"
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, players :players, :as arena} arena
        {x :x, y :y} coords
        bomb (assoc (grid/cell-bomb grid coords) :detonated {:timestamp timestamp})
        grid (grid/assoc-grid-cell grid coords :bomb bomb)
        player-coords ((:player-id bomb) players)
        player (grid/cell-player grid player-coords)
        player (players/inc-bombs player)
        grid (grid/assoc-grid-cell grid player-coords :player player)
        spread-fire #(spread-fire %1 coords %2 (:bomb-radius player) detonate-bomb timestamp)
        arena (-> (assoc arena :grid grid)
                  (spread-fire ,,, (fn [{x :x, y :y}] {:x (inc x), :y y}))
                  (spread-fire ,,, (fn [{x :x, y :y}] {:x (dec x), :y y}))
                  (spread-fire ,,, (fn [{x :x, y :y}] {:x x, :y (inc y)}))
                  (spread-fire ,,, (fn [{x :x, y :y}] {:x x, :y (dec y)})))]
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
          (grid/soft-block? cell)
          (grid/fire? cell)
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
    (if (and (some? (:hit block))
          (<= config/expiration-ms (- timestamp (:timestamp (:hit block)))))
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
        bomb (grid/cell-bomb grid coords)]
    (if (some? bomb)
      (assoc arena :grid
        (if (bombs/bomb-expired? bomb timestamp)
          (grid/dissoc-grid-cell grid coords :bomb)
          grid))
      arena)))

(defn- detonate-timed-out-bomb
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [bomb (grid/cell-bomb (:grid arena) coords)]
    (if (and (some? bomb)
          (not (contains? bomb :detonated))
          (<= config/bomb-timeout-ms (- timestamp (:timestamp bomb))))
      (detonate-bomb arena coords timestamp)
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

(defn- remove-expired-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, players :players} arena
        player (grid/cell-player grid coords)
        hit (:hit player)]
    (if (and (some? hit)
          (<= config/expiration-ms (- timestamp (:timestamp hit))))
      (assoc arena
        :grid (grid/dissoc-grid-cell grid coords :player)
        :players (assoc players (:player-id player) nil))
      arena)))

(defn- update-player
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (hit-player ,,,,,,,,,,,,,, coords timestamp)
      (remove-expired-player ,,, coords timestamp)))

(defn- remove-expired-fire
  [arena coords timestamp]
  ; {:pre [(specs/valid? ::specs/arena arena)
  ;        (specs/valid? ::specs/coords coords)
  ;        (specs/valid? ::specs/timestamp timestamp)]
  ;  :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        fire (grid/cell-fire grid coords)]
    (if (and (some? fire)
          (<= config/expiration-ms (- timestamp (:timestamp fire))))
      (assoc arena :grid (grid/dissoc-grid-cell grid coords :fire))
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
                                    (update-block ,,,,,,,,,, {:x x, :y y} timestamp)
                                    (update-bomb ,,,,,,,,,,, {:x x, :y y} timestamp)
                                    (update-player ,,,,,,,,, {:x x, :y y} timestamp)
                                    ; TODO: update-item
                                    (remove-expired-fire ,,, {:x x, :y y} timestamp))
                                (inc x))))
                        (inc y))))]
          (update-gameover arena timestamp))))
