(ns bomberman-clj.util
  (:require [bomberman-clj.config :as config]
            ; [bomberman-clj.specs :as specs]
  ))

(defn hit?
  [obj]
  (contains? obj :hit))

(defn- expired?
  [old-ts new-ts expiration-ms]
  ; {:pre [(specs/valid? ::specs/timestamp old-ts)
  ;        (specs/valid? ::specs/timestamp new-ts)]}
  (>= (- new-ts old-ts) expiration-ms))

(defn bomb-expired?
  [bomb timestamp]
  ; {:pre (specs/valid? ::specs/timestamp timestamp)}
  (and (some? bomb)
    (contains? bomb :detonated)
    (expired? (:timestamp (:detonated bomb))
      timestamp
      config/expiration-ms)))

(defn bomb-timed-out?
  [bomb timestamp]
  ; {:pre (specs/valid? ::specs/timestamp timestamp)}
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

(defn- hit-object-expired?
  [hit-object timestamp]
  ; {:pre (specs/valid? ::specs/timestamp timestamp)}
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
  [{:keys [x y], :as coords} direction]
  ; {:pre [(specs/valid? ::specs/coords coords)]}
  (case direction
    :up {:x x, :y (dec y)}
    :right {:x (inc x), :y y}
    :down {:x x, :y (inc y)}
    :left {:x (dec x), :y y}
    (do
      (println "W util::navigate - invalid direction:" direction)
      coords)))
