(ns bomberman-clj.core
  (:gen-class))

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
