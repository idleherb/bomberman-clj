(ns bomberman-clj.arena
  (:require [bomberman-clj.bombs :as bombs]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.players :as players]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]))

(defn init-arena
  "Initialize a new (width x height) arena with given players placed"
  [width height players]
  {:post [(specs/valid? ::specs/arena %)]}
  (loop [grid (grid/init-grid width height)
         players players
         player-idx 1]
    (if (> player-idx (count players))
      {:bombs {}
       :grid grid
       :players (into {} (map (fn [[player-id player]] 
                                  [player-id (:coords player)])
                          players))}
      (let [player-id (keyword (str "player-" player-idx))
            player (players/init-player (player-id players))]
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

(defn move
  "Try to move a player in the given direction"
  [arena player-id direction]
  {:pre [(specs/valid? ::specs/arena arena)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [{{v :v, :as grid} :grid, players :players} arena
        coords (player-id players)
        player (player-id (grid/cell-at grid coords))
        new-coords (util/navigate coords direction)]
    (if (and (grid/in-grid? grid new-coords)
             (grid/cell-empty? grid new-coords))
      (let [grid (grid/dissoc-grid-cell grid coords player-id)
            grid (grid/spawn grid player-id player new-coords)
            players (assoc players player-id new-coords)
            arena (assoc arena :grid grid
                               :players players)]
        arena)
      arena)))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [arena player-id timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [{{v :v, :as grid} :grid, players :players, bombs :bombs} arena
        {x :x, y :y, :as coords} (player-id players)
        player (grid/player-at grid player-id coords)]
    (if (and (players/has-bombs? player)
             (not (grid/cell-has-bomb? grid coords)))
      (let [bomb-id (keyword (str "bomb-x" x "y" y))
            bomb {:player-id player-id, :timestamp timestamp}
            bombs (assoc bombs bomb-id coords)
            player (players/dec-bombs player)
            grid (grid/assoc-grid-cell grid coords bomb-id bomb)
            grid (grid/assoc-grid-cell grid coords player-id player)]
          (assoc arena :bombs bombs
                       :grid grid))
       arena)))

(defn spread-fire
  "Spread fire along x or y axis"
  [{grid :grid, :as arena}
   {x :x, y :y, :as coords}
   transform-coords
   radius
   detonate-bomb
   timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (loop [{cur-x :x, cur-y :y} coords
          {:keys [width height], :as grid} grid]
    (if (or (= radius (Math/abs (- cur-x x)))
            (= radius (Math/abs (- cur-y y)))
            (= cur-x -1)
            (= cur-x width)
            (= cur-y -1)
            (= cur-y height))
      (assoc arena :grid grid)
      (let [bomb-id (keyword (str "bomb-x" cur-x "y" cur-y))
            bomb (grid/bomb-at grid bomb-id {:x cur-x, :y cur-y})
            arena (assoc arena :grid grid)
            arena (if (and (not (nil? bomb))
                           (not (contains? bomb :detonated)))
              (detonate-bomb arena bomb-id timestamp)
              arena)
            grid (:grid arena)]
        (recur
          (transform-coords {:x cur-x, :y cur-y})
          (grid/assoc-grid-cell grid {:x cur-x, :y cur-y} :fire {:timestamp timestamp}))))))

(defn detonate-bomb
  "Detonate a given bomb"
  [arena bomb-id timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
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
        spread-fire #(spread-fire %1 coords %2 config/bomb-radius detonate-bomb timestamp)
        arena (spread-fire arena (fn [{x :x, y :y}] {:x (inc x), :y y}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x (dec x), :y y}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x x, :y (inc y)}))
        arena (spread-fire arena (fn [{x :x, y :y}] {:x x, :y (dec y)}))]
    arena))

(defn remove-expired-bomb-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [bomb-id (cells/cell-bomb-id cell)]
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords
          (if (bombs/bomb-expired? (bomb-id cell) timestamp)
            (dissoc cell bomb-id)
            cell)))
      arena)))

(defn detonate-timed-out-bomb-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [bomb-id (cells/cell-bomb-id cell)]
      (let [bomb (bomb-id cell)]
        (if (and (not (contains? bomb :detonated))
                 (<= config/bomb-timeout-ms (- timestamp (:timestamp bomb))))
          (detonate-bomb arena bomb-id timestamp)
          arena))
      arena)))

(defn update-bomb-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (remove-expired-bomb- coords timestamp)
      (detonate-timed-out-bomb- coords timestamp)))

(defn hit-player-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [player-id (cells/cell-player-id cell)]
      (let [player (player-id cell)]
        (if (and (grid/cell-on-fire? grid coords)
                 (nil? (:hit player)))
          (assoc arena :grid
            (grid/assoc-grid-cell grid coords player-id
              (assoc (grid/player-at grid player-id coords) :hit {:timestamp timestamp})))
          arena))
      arena)))

(defn remove-expired-player-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
          (specs/valid? ::specs/coords coords)
          (specs/valid? ::specs/timestamp timestamp)]
    :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [player-id (cells/cell-player-id cell)]
      (if-let [hit (:hit (player-id cell))]
        (assoc arena
          :grid (grid/assoc-grid-cell grid coords
                  (if (<= config/player-expiration-ms (- timestamp (:timestamp hit)))
                    (dissoc cell player-id)
                    cell))
          :players (assoc (:players arena) :player-id nil))
        arena)
      arena)))

(defn update-player-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (-> arena
      (hit-player- coords timestamp)
      (remove-expired-player- coords timestamp)))

(defn update-fire-
  [arena coords timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
          (specs/valid? ::specs/coords coords)
          (specs/valid? ::specs/timestamp timestamp)]
    :post [(specs/valid? ::specs/arena %)]}
  (let [grid (:grid arena)
        cell (grid/cell-at grid coords)]
    (if-let [fire (:fire cell)]
      (assoc arena :grid
        (grid/assoc-grid-cell grid coords
          (if (<= config/fire-expiration-ms (- timestamp (:timestamp fire)))
            (dissoc cell :fire)
            cell)))
      arena)))

(defn eval-arena
  "Check if any bombs should detonate (and detonate in case). Remove expired bombs and fire."
  [arena timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [{{:keys [width height], :as grid} :grid} arena]
    (loop [arena arena y 0]
      (if (= height y)
        arena
        (recur
          (loop [arena arena x 0]
            (if (= width x)
              arena
              (recur (-> arena
                        (update-bomb-   {:x x, :y y} timestamp)
                        (update-player- {:x x, :y y} timestamp)
                        (update-fire-   {:x x, :y y} timestamp))
                     (inc x))))
            (inc y))))))
