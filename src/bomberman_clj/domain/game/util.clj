(ns bomberman-clj.domain.game.util
  (:require [bomberman-clj.config :as c]))

(defn expired?
  [old-ts new-ts expiration-ms]
  (>= (- new-ts old-ts) expiration-ms))

(defn bomb-expired?
  [bomb timestamp]
  (and (contains? bomb :detonated)
       (expired? (:timestamp (:detonated bomb))
                 timestamp
                 c/expiration-ms)))

(defn bomb-timed-out?
  [bomb timestamp]
  (and (some? bomb)
       (not (contains? bomb :detonated))
       (expired? (:timestamp bomb)
                 timestamp
                 c/bomb-timeout-ms)))

(defn fire-expired?
  [fire timestamp]
  (and (contains? fire :timestamp)
       (expired? (:timestamp fire)
                 timestamp
                 c/expiration-ms)))

(defn gameover-expired?
  [gameover timestamp]
  (and (contains? gameover :timestamp)
       (expired? (:timestamp gameover)
                 timestamp
                 c/gameover-expiration-ms)))

(defn hit?
  [obj]
  (contains? obj :hit))

(defn- hit-object-expired?
  [hit-object timestamp]
  (and (hit? hit-object)
       (expired? (:timestamp (:hit hit-object))
                 timestamp
                 c/expiration-ms)))

(defn block-expired?
  [block timestamp]
  (hit-object-expired? block timestamp))

(defn item-expired?
  [item timestamp]
  (hit-object-expired? item timestamp))

(defn player-expired?
  [player timestamp]
  (hit-object-expired? player timestamp))
 