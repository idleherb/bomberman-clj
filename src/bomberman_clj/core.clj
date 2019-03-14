(ns bomberman-clj.core
  (:gen-class))

(defn cell-idx
  "Return grid cell index from coordinates"
  [{:keys [width height v], :as grid} coords]
  (let [[x y] coords]
    (when (and (<= 0 x (dec width))
               (<= 0 y (dec height)))
      (+ (* y width) x))))

(defn in-grid?
  "Check if coordinates are within the given grid"
  [{:keys [width height v], :as grid} coords]
    (not (nil? (cell-idx grid coords))))

(defn cell-at
  "Return the cell of a grid at the given coordinates"
  [{:keys [width height v], :as grid} coords]
  (nth v (cell-idx grid coords)))

(defn cell-empty?
  "Check if a given cell is empty"
  [grid coords]
  (nil? (cell-at grid coords)))

(defn rand-coords
  "Return random coordinates within the given grid"
  [{:keys [width height v], :as grid}]
  [(rand-int width) (rand-int height)])

(defn find-empty-cell
  "Find a random empty cell within the given grid. Defaults to 100 tries."
  ([grid] (find-empty-cell grid 100))
  ([{:keys [width height v], :as grid} max-tries]
    (loop [coords (rand-coords grid)
           num-tries 1]
      (if (cell-empty? grid coords)
        coords
        (if (= max-tries num-tries)
          (throw (Exception. "failed to find empty cell"))
          (recur (rand-coords grid) (inc num-tries)))))))

(defn spawn
  "Spawn an object at the given coordinates."
  [{:keys [width height v], :as grid}, object-id, object coords]
  (if (not (cell-empty? grid coords))
    (throw (Exception. "can only spawn in empty cell"))
    (assoc grid :v (assoc v (cell-idx grid coords) {object-id object}))))

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
        {:grid grid, :players players, :bombs {}}
        (if (contains? ((keyword (str "player-" player-idx)) players) :coords)
          ; spawn player at given coords
          (let [player-id (keyword (str "player-" player-idx))
                {coords :coords, :as player} (player-id players)
                grid (spawn grid player-id player coords)
                player-idx (inc player-idx)]
            (recur grid players player-idx))
          ; spawn player at random coords
          (let [coords (find-empty-cell grid)
                player-id (keyword (str "player-" player-idx))
                {player-glyph :glyph, :as player} (player-id players)
                player (assoc player :coords coords)
                players (assoc players player-id player)
                grid (spawn grid player-id player coords)
                player-idx (inc player-idx)]
            (recur grid players player-idx)))))))

(defn navigate
  "Navigate from coordinates into the given direction"
  [[x y] direction]
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
        {coords :coords, glyph :glyph, :as player} (player-id players)
        new-coords (navigate coords direction)]
    (if (and (in-grid? grid new-coords)
             (cell-empty? grid new-coords))
      (let [player (assoc player :coords new-coords)
            players (assoc players player-id player)
            v (:v grid)
            prev-cell (cell-at grid coords)
            prev-cell (dissoc prev-cell player-id)
            prev-cell (if (empty? prev-cell) nil prev-cell)
            grid (assoc grid :v (assoc v (cell-idx grid coords) prev-cell))
            grid (spawn grid player-id player new-coords)
            arena (assoc arena :grid grid)
            arena (assoc arena :players players)]
        arena)
      arena)
    ))

(defn plant-bomb
  "Try to plant a bomb with the given player at their current coordinates"
  [arena player-id]
  (let [{{v :v, :as grid} :grid, players :players, bombs :bombs} arena
        {[x y, :as coords] :coords, :as player} (player-id players)
        cell-idx (cell-idx grid coords)
        cell (cell-at grid coords)
        bomb {:player-id player-id, :timestamp (System/currentTimeMillis)}
        bomb-cell (assoc cell :bomb bomb)
        bombs (assoc bombs (keyword (str "x" x "y" y)) (assoc bomb :coords coords))]
    (assoc arena :bombs bombs
                 :grid (assoc grid :v (assoc v cell-idx bomb-cell)))))

(defn eval-arena
  ""
  [arena]
  (let [{{v :v, :as grid} :grid, players :players} arena]
    arena))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
