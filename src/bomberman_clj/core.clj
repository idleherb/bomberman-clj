(ns bomberman-clj.core
  (:gen-class))

(defn init-arena
  "Initialize a new arena"
  []
  (into (vector) (take 10 (repeat (into (vector) (take 10 (repeat nil)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
