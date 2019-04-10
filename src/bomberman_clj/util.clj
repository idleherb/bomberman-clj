(ns bomberman-clj.util
  ; (:require [bomberman-clj.specs :as specs])
  )

(defn expired?
  [old-ts new-ts expiration-ms]
  ; {:pre [(specs/valid? ::specs/timestamp old-ts)
  ;        (specs/valid? ::specs/timestamp new-ts)]}
  (>= (- new-ts old-ts) expiration-ms))

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
