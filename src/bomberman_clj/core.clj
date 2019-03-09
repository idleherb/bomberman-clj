(ns bomberman-clj.core
  (:gen-class))

(defn spawn
  "Spawn an object at the given coordinates."
  [grid [x y]]
  (assoc (assoc grid 0 "P") 254 "P"))

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
