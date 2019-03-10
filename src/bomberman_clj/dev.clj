(ns bomberman-clj.dev
  (:require [bomberman-clj.core :refer :all]))

(defn render-cell
  [cell]
  (if (nil? cell) "." cell))

(defn render-arena
  [width height players]
  (let [arena (init-arena width height players)
        grid (:grid arena)
        v (:v grid)]
    (apply str (map-indexed
      (fn [idx cell]
        (if (= 0 (mod (inc idx) width))
          (str (render-cell cell) "\n")
          (str (render-cell cell) " ")))
      v))))
