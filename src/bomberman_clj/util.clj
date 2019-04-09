(ns bomberman-clj.util
  (:require [bomberman-clj.specs :as specs]))

(defn navigate
  "Navigate from coordinates into the given direction"
  [{:keys [x y], :as coords} direction]
  ; {:pre [(specs/valid? ::specs/coords coords)]}
  (case direction
    :north {:x x, :y (dec y)}
    :east {:x (inc x), :y y}
    :south {:x x, :y (inc y)}
    :west {:x (dec x), :y y}
    (throw (Exception. (str "invalid direction: " direction)))))

(defn expired?
  [old-ts new-ts expiration-ms]
  ; {:pre [(specs/valid? ::specs/timestamp old-ts)
  ;        (specs/valid? ::specs/timestamp new-ts)]}
  (>= (- new-ts old-ts) expiration-ms))
