(ns bomberman-clj.bombs
  (:require [bomberman-clj.specs :as specs]
            [bomberman-clj.config :as config]))

(defn bomb-expired?
  [bomb timestamp]
  {:pre (specs/valid? ::specs/timestamp timestamp)}
  (and (contains? bomb :detonated)
       (>= (- timestamp (:timestamp (:detonated bomb)))
           config/bomb-expiration-ms)))
