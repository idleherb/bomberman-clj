(ns bomberman-clj.domain.util
  (:require [bomberman-clj.config :as config]))

(defn expired?
  [old-ts new-ts expiration-ms]
  (>= (- new-ts old-ts) expiration-ms))

(defn bomb-expired?
  [bomb timestamp]
  (and (some? bomb)
    (contains? bomb :detonated)
    (expired? (:timestamp (:detonated bomb))
      timestamp
      config/expiration-ms)))

(defn bomb-timed-out?
  [bomb timestamp]
  (and (some? bomb)
    (not (contains? bomb :detonated))
    (expired? (:timestamp bomb)
      timestamp
      config/bomb-timeout-ms)))

(defn fire-expired?
  [fire timestamp]
  (and (contains? fire :timestamp)
    (expired? (:timestamp fire)
      timestamp
      config/expiration-ms)))

(defn gameover-expired?
  [gameover timestamp]
  (and (contains? gameover :timestamp)
    (expired? (:timestamp gameover)
      timestamp
      config/gameover-expiration-ms)))

(defn hit?
  [obj]
  (contains? obj :hit))

(defn- hit-object-expired?
  [hit-object timestamp]
  (and (hit? hit-object)
    (expired? (:timestamp (:hit hit-object))
      timestamp
      config/expiration-ms)))

(defn block-expired?
  [block timestamp]
  (hit-object-expired? block timestamp))

(defn item-expired?
  [item timestamp]
  (hit-object-expired? item timestamp))

(defn player-expired?
  [player timestamp]
  (hit-object-expired? player timestamp))

(defn navigate
  [coords direction]
  (let [{:keys [x y]} coords]
    (case direction
      :up    {:x x,       :y (dec y)}
      :right {:x (inc x), :y y}
      :down  {:x x,       :y (inc y)}
      :left  {:x (dec x), :y y}
      coords)))
