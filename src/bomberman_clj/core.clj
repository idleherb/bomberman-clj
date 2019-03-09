(ns bomberman-clj.core
  (:gen-class))

(defn spawn
  "Spawn an object at the given coordinates."
  [{:keys [width height v] :as grid} {:keys [symbol coords] :as object}]
  (println "core::spawn -" "width:" width "height:" height "symbol:" symbol "coords:" coords)
  (let [[x y] coords]
    (println "core::spawn -""x:" x "y:" y, "i:" (+ (* x height) y))
    (assoc grid :v (assoc v (+ (* x height) y) symbol))))

(defn init-arena
  "Initialize a new (width x height) arena"
  [width height]
  (let [grid (into (vector) (take (* width height) (repeat nil)))]
    {:grid grid
     :players [0 0]}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
