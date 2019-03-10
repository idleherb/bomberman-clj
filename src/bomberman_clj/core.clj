(ns bomberman-clj.core
  (:gen-class))

(defn cell-at
  "Return the cell of a grid at the given coordinates"
  [{:keys [width height v] :as grid} coords]
  (let [[x y] coords]
    (nth v (+ (* x height) y))))

(defn cell-empty?
  "Check if a given cell is empty"
  [grid coords]
  (nil? (cell-at grid coords)))

(defn rand-coords
  "Return random coordinates within the given grid"
  [{:keys [width height v] :as grid}]
  [(rand-int width) (rand-int height)])

(defn find-empty-cell
  "Find a random empty cell within the given grid. Defaults to 100 tries."
  ([grid] (find-empty-cell grid 100))
  ([{:keys [width height v] :as grid} max-tries]
    (loop [coords (rand-coords grid)
            num-tries 1]
      (if (cell-empty? grid coords)
        (do
          (println "D core::find-empty-cell - took" num-tries "tries")
          coords)
        (if (= max-tries num-tries)
          (throw (Exception. "failed to find empty cell"))
          (recur (rand-coords grid) (inc num-tries)))))))

(defn spawn
  "Spawn an object at the given coordinates."
  [{:keys [width height v] :as grid} {:keys [symbol coords] :as object}]
  (let [[x y] coords]
    (if (not (cell-empty? grid coords))
      (throw (Exception. "can only spawn in empty cell"))
      (assoc grid :v (assoc v (+ (* x height) y) symbol)))))

(defn init-arena
  "Initialize a new (width x height) arena with given players placed"
  [width height players]
  (let [grid {:width width, :height height, :v (into (vector) (take (* width height) (repeat nil)))}]
    (loop [grid grid
           players players
           player-idx 0]
      (if (= player-idx (count players))
        {:grid grid, :players players}
        (let [coords (find-empty-cell grid)
              {player-symbol :symbol} (nth players player-idx)
              player {:symbol player-symbol, :coords coords}
              players (assoc players player-idx player)
              grid (spawn grid player)
              player-idx (inc player-idx)]
          (recur grid players player-idx))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
