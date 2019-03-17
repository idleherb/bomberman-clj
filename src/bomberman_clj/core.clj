(ns bomberman-clj.core
  (:gen-class))

(def bomb-timeout-ms 10000)
(def bomb-radius 3)

(defn cell-idx
  "Return grid cell index from coordinates"
  [{:keys [width height], :as grid} [x y, :as coords]]
  (when (and (<= 0 x (dec width))
             (<= 0 y (dec height)))
    (+ (* y width) x)))

(defn cell-at
  "Return the grid cell at the given coordinates"
  [{v :v, :as grid} coords]
  (nth v (cell-idx grid coords)))

(defn assoc-grid-cell
  ([{v :v, :as grid} coords key val]
    (assoc grid :v
      (assoc v (cell-idx grid coords)
        (assoc (cell-at grid coords) key val))))
  ([{v :v, :as grid} coords cell]
    (assoc grid :v
      (assoc v (cell-idx grid coords) cell))))

(defn dissoc-grid-cell
  "Like dissoc, but returns nil if the cell is empty afterwards."
  [{v :v, :as grid} coords k]
  (let [cell (dissoc (cell-at grid coords) k)
        cell (if (empty? cell) nil cell)]
    (assoc-grid-cell grid coords cell)))

(defn in-grid?
  "Check if coordinates are within the given grid"
  [grid coords]
  (not (nil? (cell-idx grid coords))))

(defn cell-player-id
  "Checks if the given cell contains a player and returns its mapping, else nil"
  [cell]
  (first ; player-id (or nil)
    (first ; first (or nil) [player-id, player]
      (filter (fn [[k _]] (re-matches #"player-\d+" (name k))) cell))))

(defn cell-player
  "Return the player in the given cell if any"
  ([cell] (cell-player cell (cell-player-id cell)))
  ([cell player-id] (player-id cell)))

(defn cell-empty?
  "Check if a given cell is empty"
  [grid coords]
  (nil? (cell-at grid coords)))

(defn rand-coords
  "Return random coordinates within the given grid"
  [{:keys [width height], :as grid}]
  [(rand-int width) (rand-int height)])

(defn find-empty-cell
  "Find a random empty cell within the given grid. Defaults to 100 tries."
  ([grid] (find-empty-cell grid 100))
  ([grid max-tries]
    (loop [coords (rand-coords grid)
           num-tries 1]
      (if (cell-empty? grid coords)
        coords
        (if (= max-tries num-tries)
          (throw (Exception. "failed to find empty cell"))
          (recur (rand-coords grid) (inc num-tries)))))))

(defn spawn
  "Spawn an object at the given coordinates."
  [{v :v, :as grid}
   object-id
   object
   coords]
  (if (not (cell-empty? grid coords))
    (throw (Exception. "can only spawn in empty cell"))
    (assoc-grid-cell grid coords object-id object)))

(defn init-arena
  "Initialize a new (width x height) arena with given players placed"
  [width height players]
  (let [grid {:width width,
              :height height,
              :v (into (vector) (take (* width height) (repeat nil)))}]
    (loop [grid grid
           players players
           player-idx 1]
      (if (> player-idx (count players))
        {:bombs {}
         :grid grid
         :players (into {} (map (fn [[k v]] [k (:coords v)])) players)}
        (let [player-id (keyword (str "player-" player-idx))]
          (if (contains? (player-id players) :coords)
            ; spawn player at given coords
            (let [{coords :coords, :as player} (player-id players)
                  grid (spawn grid player-id (dissoc player :coords) coords)
                  player-idx (inc player-idx)]
              (recur grid players player-idx))
            ; spawn player at random coords
            (let [coords (find-empty-cell grid)
                  grid (spawn grid player-id (player-id players) coords)
                  players (assoc players player-id {:coords coords})
                  player-idx (inc player-idx)]
              (recur grid players player-idx))))))))

(defn navigate
  "Navigate from coordinates into the given direction"
  [[x y, :as coords] direction]
  (case direction
    :north [x (dec y)]
    :east [(inc x) y]
    :south [x (inc y)]
    :west [(dec x) y]
    (throw (Exception. (str "invalid direction: " direction)))))

(defn move
  "Try to move a player in the given direction"
  [arena player-id direction]
  (let [{{v :v, :as grid} :grid, players :players} arena
        coords (player-id players)
        player (player-id (cell-at grid coords))
        new-coords (navigate coords direction)]
    (if (and (in-grid? grid new-coords)
             (cell-empty? grid new-coords))
      (let [grid (dissoc-grid-cell grid coords player-id)
            grid (spawn grid player-id player new-coords)
            players (assoc players player-id new-coords)
            arena (assoc arena
              :grid grid
              :players players)]
        arena)
      arena)))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [arena player-id]
  (let [{{v :v, :as grid} :grid, players :players, bombs :bombs} arena
        [x y, :as coords] (player-id players)
        bomb {:timestamp (System/currentTimeMillis)}  ; TODO: make timestamp parameter
        bomb-id (keyword (str "bomb-x" x "y" y))
        bombs (assoc bombs bomb-id coords)]
    (assoc arena
      :bombs bombs
      :grid (assoc-grid-cell grid coords bomb-id bomb))))

(defn spread-fire
  "Spread fire along x or y axis"
  [grid
   [x y, :as coords]
   transform-coords
   radius]
  (loop [[cur-x cur-y] coords
         {:keys [v width height], :as grid} grid
         break false]
    (if (or (true? break)
            (= radius (Math/abs (- cur-x x)))
            (= radius (Math/abs (- cur-y y)))
            (= cur-x -1)
            (= cur-x width)
            (= cur-y -1)
            (= cur-y height))
      grid
      (let [cell (cell-at grid [cur-x cur-y])
            player-id (cell-player-id cell)]
        (recur
          (transform-coords [cur-x cur-y])
          (assoc-grid-cell grid [cur-x cur-y] :fire true)
          (not (nil? player-id)))))))

(defn detonate-bomb
  "Detonate a given bomb"
  [arena bomb-id]
  (let [{{:keys [v width height], :as grid} :grid
         players :players
         bombs :bombs
         :as arena} arena
        [x y, :as coords] (bomb-id bombs)
        grid (spread-fire grid coords (fn [[x y]] [(inc x) y]) bomb-radius)
        grid (spread-fire grid coords (fn [[x y]] [(dec x) y]) bomb-radius)
        grid (spread-fire grid coords (fn [[x y]] [x (inc y)]) bomb-radius)
        grid (spread-fire grid coords (fn [[x y]] [x (dec y)]) bomb-radius)
        grid (dissoc-grid-cell grid coords bomb-id)
        bombs (dissoc bombs bomb-id)
        arena (assoc arena
          :bombs bombs
          :grid grid)]
    arena))

(defn eval-arena
  "Check if any bombs should detonate (and detonate in case)"
  [arena timestamp]
  (let [{bombs :bombs, :as arena} arena
        ; update bombs
        arena (loop [idx 0 {grid :grid, :as arena} arena]
          (if (= idx (count bombs))
            arena
            (let [bomb-id (nth (keys bombs) idx)
                  bomb-coords (bomb-id bombs)
                  bomb (bomb-id (cell-at grid bomb-coords))
                  arena (if (<= bomb-timeout-ms (- timestamp (:timestamp bomb)))
                    (detonate-bomb arena bomb-id)
                    arena)]
              (recur (inc idx) arena))))
        {grid :grid, players :players} arena
        ; update players
        grid (loop [idx 0 grid grid]
          (if (= idx (count players))
            grid
            (let [player-id (nth (keys players) idx)
                  player-coords (player-id players)
                  cell (cell-at grid player-coords)
                  grid (if (contains? cell :fire)
                    (assoc-grid-cell grid player-coords player-id
                      (assoc (player-id cell) :hit true))
                    grid)]
              (recur (inc idx) grid))))
        arena (assoc arena :grid grid)]
      arena
    ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
