(ns bomberman-clj.grid
  (:require [bomberman-clj.cells :as cells]
            [bomberman-clj.specs :as specs]))

(defn init-grid
  [width height]
  {:post (specs/valid? ::specs/grid %)}
  {:width width,
   :height height,
   :v (into (vector) (take (* width height) (repeat nil)))}) ; TODO: doall

(defn cell-idx
  "Return grid cell index from coordinates"
  [{:keys [width height], :as grid}
   {:keys [x y], :as coords}]
  {:pre [(specs/valid? ::specs/grid grid)
         (specs/valid? ::specs/coords coords)]}
  (when (and (<= 0 x (dec width))
             (<= 0 y (dec height)))
    (+ (* y width) x)))

(defn cell-at
  "Return the grid cell at the given coordinates"
  [{v :v, :as grid} coords]
  {:pre [(specs/valid? ::specs/grid grid)
         (specs/valid? ::specs/coords coords)]
   :post [(specs/valid? ::specs/cell %)]}
  (nth v (cell-idx grid coords)))

(defn cell-has-bomb?
  [grid coords]
  (not (nil? (cells/cell-bomb (cell-at grid coords)))))

(defn cell-on-fire?
  [grid coords]
  (contains? (cell-at grid coords) :fire))

(defn- object-at
  [grid object-id coords]
  (object-id (cell-at grid coords)))

(defn player-at
  [grid player-id coords]
  (object-at grid player-id coords))

(defn bomb-at
  [grid bomb-id coords]
  (object-at grid bomb-id coords))

(defn assoc-grid-cell
  ([{v :v, :as grid} coords key val]
    {:pre [(specs/valid? ::specs/grid grid)
           (specs/valid? ::specs/coords coords)]
     :post [(specs/valid? ::specs/grid %)]}
    (assoc grid :v
      (assoc v (cell-idx grid coords)
        (assoc (cell-at grid coords) key val))))
  ([{v :v, :as grid} coords cell]
    {:pre [(specs/valid? ::specs/grid grid)
           (specs/valid? ::specs/coords coords)]
     :post [(specs/valid? ::specs/grid %)]}
    (assoc grid :v
      (assoc v (cell-idx grid coords) cell))))

(defn dissoc-grid-cell
  "Like dissoc, but returns nil if the cell is empty afterwards."
  [{v :v, :as grid} coords k]
  {:pre [(specs/valid? ::specs/grid grid)
         (specs/valid? ::specs/coords coords)]
   :post [(specs/valid? ::specs/grid %)]}
  (let [cell (dissoc (cell-at grid coords) k)
        cell (if (empty? cell) nil cell)]
    (assoc-grid-cell grid coords cell)))

(defn in-grid?
  "Check if coordinates are within the given grid"
  [grid coords]
  {:pre [(specs/valid? ::specs/grid grid)
         (specs/valid? ::specs/coords coords)]}
  (not (nil? (cell-idx grid coords))))

(defn cell-empty?
  "Check if a given cell is empty"
  [grid coords]
  {:pre [(specs/valid? ::specs/grid grid)
          (specs/valid? ::specs/coords coords)]}
  (let [cell (cell-at grid coords)]
    (or (nil? cell)
        (and (nil? (cells/cell-player cell))
             (nil? (cells/cell-bomb cell))))))

(defn rand-coords
  "Return random coordinates within the given grid"
  [{:keys [width height], :as grid}]
  {:pre [(specs/valid? ::specs/grid grid)]
   :post [(specs/valid? ::specs/coords %)]}
  {:x (rand-int width), :y (rand-int height)})

(defn find-empty-cell
  "Find a random empty cell within the given grid. Defaults to 100 tries."
  ([grid] (find-empty-cell grid 100))
  ([grid max-tries]
    {:pre [(specs/valid? ::specs/grid grid)]
     :post [(specs/valid? ::specs/coords %)]}
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
  {:pre [(specs/valid? ::specs/grid grid)
         (specs/valid? ::specs/coords coords)]
   :post [(specs/valid? ::specs/grid %)]}
  (if (not (cell-empty? grid coords))
    (throw (Exception. "can only spawn in empty cell"))
    (assoc-grid-cell grid coords object-id object)))
