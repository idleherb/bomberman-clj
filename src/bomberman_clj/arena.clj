(ns bomberman-clj.arena
  (:require [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.players :as players]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]))

(defn- init-player
  "Add default properties to the given player"
  [player]
  (merge player {:bomb-count config/bomb-count}))

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
            player (init-player (player-id players))]
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
        [x y, :as coords] (player-id players)
        player (grid/player-at grid player-id coords)]
    (if (and (players/has-bombs? player)
             (not (grid/cell-has-bomb? grid coords)))
      (let [bomb-id (keyword (str "bomb-x" x "y" y))
            bomb {:timestamp timestamp}
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
    [x y, :as coords]
    transform-coords
    radius
    detonate-bomb
    timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/coords coords)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (loop [[cur-x cur-y] coords
          {:keys [width height], :as grid} grid]
    (if (or (= radius (Math/abs (- cur-x x)))
            (= radius (Math/abs (- cur-y y)))
            (= cur-x -1)
            (= cur-x width)
            (= cur-y -1)
            (= cur-y height))
      (assoc arena :grid grid)
      (let [bomb-id (keyword (str "bomb-x" cur-x "y" cur-y))
            bomb (grid/bomb-at grid bomb-id [cur-x cur-y])
            arena (assoc arena :grid grid)
            arena (if (and (not (nil? bomb))
                           (not (contains? bomb :detonated)))
              (detonate-bomb arena bomb-id timestamp)
              arena)
            grid (:grid arena)]
        (recur
          (transform-coords [cur-x cur-y])
          (grid/assoc-grid-cell grid [cur-x cur-y] :fire {:timestamp timestamp}))))))

(defn detonate-bomb
  "Detonate a given bomb"
  [arena bomb-id timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [{grid :grid, bombs :bombs, :as arena} arena
        [x y, :as coords] (bomb-id bombs)
        bomb (assoc (grid/bomb-at grid bomb-id coords) :detonated {:timestamp timestamp})
        grid (grid/assoc-grid-cell grid coords bomb-id bomb)
        arena (assoc arena :grid grid)
        spread-fire #(spread-fire %1 coords %2 config/bomb-radius detonate-bomb timestamp)
        arena (spread-fire arena (fn [[x y]] [(inc x) y]))
        arena (spread-fire arena (fn [[x y]] [(dec x) y]))
        arena (spread-fire arena (fn [[x y]] [x (inc y)]))
        arena (spread-fire arena (fn [[x y]] [x (dec y)]))]
    arena))

(defn eval-arena
  "Check if any bombs should detonate (and detonate in case)"
  [arena timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [; update bombs
        arena (loop [idx 0 {bombs :bombs, grid :grid, :as arena} arena]
          (if (= idx (count bombs))
            arena
            (let [bomb-id (nth (keys bombs) idx)
                  bomb-coords (bomb-id bombs)
                  bomb (grid/bomb-at grid bomb-id bomb-coords)
                  arena (if (and (<= config/bomb-timeout-ms (- timestamp (:timestamp bomb)))
                                 (not (contains? bomb :detonated)))
                    (detonate-bomb arena bomb-id timestamp)
                    arena)]
              (recur (inc idx) arena))))
        {grid :grid, players :players} arena
        ; update players
        grid (loop [idx 0 grid grid]
          (if (= idx (count players))
            grid
            (let [player-id (nth (keys players) idx)
                  player-coords (player-id players)
                  grid (if (grid/cell-on-fire? grid player-coords)
                    (grid/assoc-grid-cell grid player-coords player-id
                      (assoc (grid/player-at grid player-id player-coords) :hit {:timestamp timestamp}))
                    grid)]
              (recur (inc idx) grid))))
        arena (assoc arena :grid grid)]
      arena
    ))
