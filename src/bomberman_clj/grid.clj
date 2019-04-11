(ns bomberman-clj.grid
  (:require [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            ; [bomberman-clj.specs :as specs]
  ))

(defn- idx-coords
  [width height idx]
  {:x (mod idx width), :y (int (/ idx width))})

(defn init
  [width height]
  ; {:post (specs/valid? ::specs/grid %)}
  {:width width,
   :height height,
   :v (into (vector) (map-indexed (fn [i _]
    (let [{:keys [x y]} (idx-coords width height i)]
      (if (and (odd? x) (odd? y))
        {:block {:type :hard}}
        (when (< (rand) 4/10) {:block {:type :soft}}))))
    (take (* width height) (repeat nil))))
   })

(defn- cell-idx
  "Return grid cell index from coordinates"
  [{:keys [width height], :as grid}
   {:keys [x y], :as coords}]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]}
  (when (and (<= 0 x (dec width))
             (<= 0 y (dec height)))
    (+ (* y width) x)))

(defn cell-at
  "Return the grid cell at the given coordinates"
  [{v :v, :as grid} coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]
  ;  :post [(specs/valid? ::specs/cell %)]}
  (nth v (cell-idx grid coords)))

(defn- object-at
  [grid object-id coords]
  (object-id (cell-at grid coords)))

(defn bomb-at
  ([grid coords]
   (cells/cell-bomb (cell-at grid coords)))
  ([grid bomb-id coords]
   (object-at grid bomb-id coords)))

(defn bomb-id-at
  [grid coords]
  (cells/cell-bomb-id (cell-at grid coords)))

(defn bomb?
  [grid coords]
  (some? (cells/cell-bomb (cell-at grid coords))))

(defn fire?
  [grid coords]
  (contains? (cell-at grid coords) :fire))

(defn block?
  [grid coords]
  (contains? (cell-at grid coords) :block))

(defn hard-block?
  [grid coords]
  (let [cell (cell-at grid coords)]
    (= :hard (:type (:block cell)))))

(defn soft-block?
  [grid coords]
  (let [cell (cell-at grid coords)]
    (= :soft (:type (:block cell)))))

(defn player-at
  ([grid coords]
   (cells/cell-player (cell-at grid coords)))
  ([grid player-id coords]
   (object-at grid player-id coords)))

(defn player-id-at
  [grid coords]
  (cells/cell-player-id (cell-at grid coords)))

(defn item-at
  [grid coords]
  (:item (cell-at grid coords)))

(defn item?
  [grid coords]
  (contains? (cell-at grid coords) :item))

(defn assoc-grid-cell
  ([{v :v, :as grid} coords cell]
    ; {:pre [(specs/valid? ::specs/grid grid)
    ;        (specs/valid? ::specs/coords coords)]
    ;  :post [(specs/valid? ::specs/grid %)]}
    (assoc grid :v
      (assoc v (cell-idx grid coords) cell)))
  ([{v :v, :as grid} coords key val]
    ; {:pre [(specs/valid? ::specs/grid grid)
    ;        (specs/valid? ::specs/coords coords)]
    ;  :post [(specs/valid? ::specs/grid %)]}
    (assoc-grid-cell grid coords
      (assoc (cell-at grid coords) key val))))

(defn dissoc-grid-cell
  "Like dissoc, but returns nil if the cell is empty afterwards."
  [{v :v, :as grid} coords k]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]
  ;  :post [(specs/valid? ::specs/grid %)]}
  (let [cell (dissoc (cell-at grid coords) k)
        cell (if (empty? cell) nil cell)]
    (assoc-grid-cell grid coords cell)))

(defn in-grid?
  "Check if coordinates are within the given grid"
  [grid coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]}
  (some? (cell-idx grid coords)))

(defn cell-empty?
  "Check if a given cell is empty"
  [grid coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]}
  (let [cell (cell-at grid coords)]
    (or (nil? cell)
        (and (nil? (cells/cell-player cell))
             (nil? (cells/cell-bomb cell))
             (nil? (:block cell))
             (nil? (:item cell))))))

(defn rand-coords
  [{:keys [width height], :as grid}]
  ; {:pre [(specs/valid? ::specs/grid grid)]
  ;  :post [(specs/valid? ::specs/coords %)]}
  {:x (rand-int width), :y (rand-int height)})

(defn find-empty-cell
  [grid]
  ; {:pre [(specs/valid? ::specs/grid grid)]
  ;  :post [(specs/valid? ::specs/coords %)]}
  (loop [num-tries 1]
    (let [coords (rand-coords grid)]
      (if (cell-empty? grid coords)
        coords
        (if (= num-tries config/spawn-max-tries)
          (println "W grid::find-empty-cell - failed to find empty cell")
          (recur (inc num-tries)))))))

(defn spawn
  [grid object-id object coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]
  ;  :post [(specs/valid? ::specs/grid %)]}
  (assoc-grid-cell grid coords object-id object))
