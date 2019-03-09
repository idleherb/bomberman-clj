(ns bomberman-clj.core
  (:gen-class))

(defn init-arena
  "Initialize a new arena"
  []
  [
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
    [nil nil nil nil nil nil nil nil nil nil]
  ])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
