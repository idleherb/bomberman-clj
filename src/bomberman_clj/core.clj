(ns bomberman-clj.core
  (:gen-class))

(defn init-arena
  "Initialize a new (w x h) arena"
  [w h]
  (into (vector) (take w (repeat (into (vector) (take h (repeat nil)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
