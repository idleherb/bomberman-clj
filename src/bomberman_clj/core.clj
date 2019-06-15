(ns bomberman-clj.core
  (:require [bomberman-clj.server.core :as server])
  (:gen-class))

(defn -main
  ([host port]
    (server/run host port))
  ([]
    (-main "0.0.0.0" "8080")))
