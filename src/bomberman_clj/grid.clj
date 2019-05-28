(ns bomberman-clj.grid
  (:require [bomberman-clj.config :as config]
            ; [bomberman-clj.specs :as specs]
  ))

(defn- idx-coords
  [width height idx]
  {:x (mod idx width), :y (int (/ idx width))})

(defn- coords-idx
  "Return grid cell index from coordinates"
  [grid coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]}
  (let [{:keys [width height]} grid
        {:keys [x y]} coords]
    (when (and (<= 0 x (dec width))
               (<= 0 y (dec height)))
      (+ (* y width) x))))

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

(defn cell-at
  "Return the grid cell at the given coordinates"
  [{v :v, :as grid} coords]
  ; {:pre [(specs/valid? ::specs/grid grid)
  ;        (specs/valid? ::specs/coords coords)]
  ;  :post [(specs/valid? ::specs/cell %)]}
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
    ; {:pre [(specs/valid? ::specs/grid grid)
    ;        (specs/valid? ::specs/coords coords)]
    ;  :post [(specs/valid? ::specs/grid %)]}
    (assoc grid :v
      (assoc (:v grid) (coords-idx grid coords) cell)))
  ([grid coords key val]
    ; {:pre [(specs/valid? ::specs/grid grid)
    ;        (specs/valid? ::specs/coords coords)]
    ;  :post [(specs/valid? ::specs/grid %)]}
    (assoc-grid-cell grid coords
      (assoc (cell-at grid coords) key val))))

(defn dissoc-grid-cell
  "Like dissoc, but returns nil if the cell is empty afterwards."
  [grid coords k]
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
  (some? (coords-idx grid coords)))

(defn cell-empty?
  "Check if a given cell is empty"
  ([cell]
    (or (nil? cell)
      (and (not (block? cell))
        (not (bomb? cell))
        (not (item? cell))
        (not (player? cell)))))
  ([grid coords]
    ; {:pre [(specs/valid? ::specs/grid grid)
    ;        (specs/valid? ::specs/coords coords)]}
    (cell-empty? (cell-at grid coords))))

(defn- rand-coords
  [grid]
  ; {:pre [(specs/valid? ::specs/grid grid)]
  ;  :post [(specs/valid? ::specs/coords %)]}
  (let [{:keys [width height]} grid]
    {:x (rand-int width), :y (rand-int height)}))

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
