(ns bomberman-clj.core
  (:require [bomberman-clj.server.core :as s])
  (:gen-class))

(defn -main
  ([host port]
    (s/run host port))
  ([]
    (-main "0.0.0.0" "8080")))
