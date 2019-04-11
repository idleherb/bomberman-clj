(ns bomberman-clj.bombs
  (:require [bomberman-clj.config :as config]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]))

(defn bomb-expired?
  [bomb timestamp]
  ; {:pre (specs/valid? ::specs/timestamp timestamp)}
  (and (contains? bomb :detonated)
       (util/expired? (:timestamp (:detonated bomb))
                      timestamp
                      config/expiration-ms)))
