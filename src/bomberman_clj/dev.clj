(ns bomberman-clj.dev
  (:require [bomberman-clj.core :refer :all])
  (:require [lanterna.screen :as s]))

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

(defn arena-rows
  [arena]
  (let [grid (:grid arena)
        {v :v, width :width, height :height} grid]
    (loop [row-idx 0
           rows []]
      (if (= row-idx height)
        rows
        (let [start-idx (* width row-idx)
              end-idx (+ start-idx width)
              row (subvec v start-idx end-idx)]
          (recur (inc row-idx)
                 (conj rows row)))
      ))
    ))

(defn draw-arena
  [width height players]
  (let [scr (s/get-screen :swing)
        arena (init-arena width height players)
        rows (arena-rows arena)
        {[x y] :coords} (:player-1 (:players arena))]
    (s/in-screen scr
      (doseq [[row-idx row] (map-indexed vector rows)]
        (doseq [[char-idx char] (map-indexed vector row)]
          (s/put-string scr       ; screen
                        (if (= 0 char-idx) char-idx (* 2 char-idx))  ; x
                        row-idx  ; y
                        (if (nil? char) "." char)  ; string
                        {:fg (if (nil? char) :green :white)})))  ; options
      (println (:players arena))
      (s/move-cursor scr (* 2 x) y)
      (s/redraw scr)
      (s/get-key-blocking scr))
    ))
