(ns bomberman-clj.core
  (:gen-class))

(defn init-arena
  "Initialize a new (width x height) arena"
  [width height]
  (into (vector) (take width (repeat (into (vector) (take height (repeat nil)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
