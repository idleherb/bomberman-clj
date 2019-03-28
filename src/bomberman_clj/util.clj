(ns bomberman-clj.util
  (:require [bomberman-clj.specs :as specs]))

(defn navigate
  "Navigate from coordinates into the given direction"
  [[x y, :as coords] direction]
  {:pre [(specs/valid? ::specs/coords coords)]}
  (case direction
    :north [x (dec y)]
    :east [(inc x) y]
    :south [x (inc y)]
    :west [(dec x) y]
    (throw (Exception. (str "invalid direction: " direction)))))
