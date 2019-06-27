(ns bomberman-clj.domain.game.grid
  (:require [bomberman-clj.config :as c]))

(defn- idx-coords
  [width height idx]
  {:x (mod idx width), :y (int (/ idx width))})

(defn- coords-idx
  "Return grid cell index from coordinates"
  [grid coords]
  (let [{:keys [width height]} grid
        {:keys [x y]} coords]
    (when (and (<= 0 x (dec width))
               (<= 0 y (dec height)))
      (+ (* y width) x))))

(defn init
  [width height]
  {:width width,
   :height height,
   :v (into (vector) (map-indexed (fn [i _]
    (let [{:keys [x y]} (idx-coords width height i)]
      (if (and (odd? x) (odd? y))
        {:block {:type :hard}}
        (when (< (rand) 4/10) {:block {:type :soft}}))))
    (take (* width height) (repeat nil))))
   })

(defn cell-at
  "Return the grid cell at the given coordinates"
  [{v :v, :as grid} coords]
  (nth v (coords-idx grid coords)))

(defn cell-bomb
  ([cell]
   (:bomb cell))
  ([grid coords]
   (cell-bomb (cell-at grid coords))))

(defn bomb?
  ([cell]
   (some? (cell-bomb cell)))
  ([grid coords]
   (bomb? (cell-at grid coords))))

(defn cell-fire
  ([cell]
  (:fire cell))
  ([grid coords]
  (cell-fire (cell-at grid coords))))

(defn fire?
  ([cell]
    (some? (cell-fire cell)))
  ([grid coords]
    (fire? (cell-at grid coords))))

(defn cell-block
  ([cell]
    (:block cell))
  ([grid coords]
    (cell-block (cell-at grid coords))))

(defn block?
  ([cell]
    (some? (cell-block cell)))
  ([grid coords]
    (block? (cell-at grid coords))))

(defn cell-hard-block
  ([cell]
    (let [block (cell-block cell)]
      (when (= :hard (:type block)) block)))
  ([grid coords]
    (cell-hard-block (cell-at grid coords))))

(defn hard-block?
  ([cell]
    (some? (cell-hard-block cell)))
  ([grid coords]
    (hard-block? (cell-at grid coords))))

(defn cell-soft-block
  ([cell]
    (let [block (cell-block cell)]
      (when (= :soft (:type block)) block)))
  ([grid coords]
    (cell-soft-block (cell-at grid coords))))

(defn soft-block?
  ([cell]
    (some? (cell-soft-block cell)))
  ([grid coords]
    (soft-block? (cell-at grid coords))))

(defn cell-player-id
  ([cell]
    (:player-id cell))
  ([grid coords]
    (cell-player-id (cell-at grid coords))))

(defn player?
  ([cell]
    (some? (cell-player-id cell)))
  ([grid coords]
    (player? (cell-at grid coords))))

(defn cell-item
  ([cell]
    (:item cell))
  ([grid coords]
    (cell-item (cell-at grid coords))))

(defn item?
  ([cell]
    (some? (cell-item cell)))
  ([grid coords]
    (item? (cell-at grid coords))))

(defn assoc-grid-cell
  ([grid coords cell]
    (assoc grid :v
      (assoc (:v grid) (coords-idx grid coords) cell)))
  ([grid coords key val]
    (assoc-grid-cell grid coords
      (assoc (cell-at grid coords) key val))))

(defn dissoc-grid-cell
  "Like dissoc, but returns nil if the cell is empty afterwards."
  [grid coords k]
  (let [cell (dissoc (cell-at grid coords) k)
        cell (if (empty? cell) nil cell)]
    (assoc-grid-cell grid coords cell)))

(defn- in-grid?
  "Check if coordinates are within the given grid"
  [grid coords]
  (some? (coords-idx grid coords)))

(defn navigate
  [coords grid direction]
  (let [{:keys [x y]} coords
        new-coords (case direction
                     :up    {:x x,       :y (dec y)}
                     :right {:x (inc x), :y y}
                     :down  {:x x,       :y (inc y)}
                     :left  {:x (dec x), :y y}
                     coords)]
    (if (in-grid? grid new-coords)
      new-coords
      coords)))

(defn cell-empty?
  "Check if a given cell is empty"
  ([cell]
    (or (nil? cell)
      (and (not (block? cell))
        (not (bomb? cell))
        (not (item? cell))
        (not (player? cell)))))
  ([grid coords]
    (cell-empty? (cell-at grid coords))))

(defn- rand-coords
  [grid]
  (let [{:keys [width height]} grid]
    {:x (rand-int width), :y (rand-int height)}))

(defn- all-save-coords
  [grid coords]
  (let [{:keys [x y]} coords
        pairs [[{:x x, :y (- y 1)} {:x x, :y (+ y 1)}]
               [{:x x, :y (- y 1)} {:x x, :y (- y 2)}]
               [{:x x, :y (+ y 1)} {:x x, :y (+ y 2)}]
               
               [{:x (- x 1), :y y} {:x (+ x 1), :y y}]
               [{:x (- x 1), :y y} {:x (- x 2), :y y}]
               [{:x (+ x 1), :y y} {:x (+ x 2), :y y}]
               
               [{:x x,       :y (+ y 1)} {:x (+ x 1), :y y}]
               [{:x (+ x 1), :y y}       {:x x,       :y (- y 1)}]
               [{:x x,       :y (- y 1)} {:x (- x 1), :y y}]
               [{:x (- x 1), :y y}       {:x x,       :y (+ y 1)}]
               
               [{:x x,       :y (+ y 1)} {:x (+ x 1), :y (+ y 1)}]
               [{:x (+ x 1), :y y}       {:x (+ x 1), :y (- y 1)}]
               [{:x x,       :y (- y 1)} {:x (- x 1), :y (- y 1)}]
               [{:x (- x 1), :y y}       {:x (- x 1), :y (+ y 1)}]]]
    (into [] (filter #(and (in-grid? grid (first %))
                           (in-grid? grid (second %))) pairs))))

(defn- assoc-player
  [grid coords player-id]
  (assoc-in grid [:v (coords-idx grid coords)] {:player-id player-id}))

(defn- assoc-nil
  [grid coords]
  (assoc-in grid [:v (coords-idx grid coords)] nil))

(defn spawn-player
  [grid player]
  (loop [num-tries 1]
    (let [coords (rand-coords grid)
          [c1 c2] (rand-nth (all-save-coords grid coords))]
      (if (or (hard-block? grid coords)
              (hard-block? grid c1)
              (hard-block? grid c2)
              (player? grid c1)
              (player? grid c2))
        (if (= num-tries c/player-spawn-max-num-tries)
          (println "E d.g.grid::spawn-player - couldn't find a spot to spawn player")
          (recur (inc num-tries)))
        (let [grid (-> grid
                       (assoc-player coords (:player-id player))
                       (assoc-nil c1)
                       (assoc-nil c2))
              player (assoc player :coords coords)]
          [grid player])))))
