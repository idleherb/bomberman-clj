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

(defn key-to-player-direction
  [key]
  (case key
    :up {:player-id :player-1 :direction :north}
    \w {:player-id :player-2 :direction :north}
    :right {:player-id :player-1 :direction :east}
    \d {:player-id :player-2 :direction :east}
    :down {:player-id :player-1 :direction :south}
    \s {:player-id :player-2 :direction :south}
    :left {:player-id :player-1 :direction :west}
    \a {:player-id :player-2 :direction :west}
    nil))

(defn draw-arena
  [width height players]
  (let [scr (s/get-screen :swing)
        arena (init-arena width height players)]
    (s/in-screen scr
      (loop [arena arena
             key nil]
        (when (not= key :escape)
          (let [{player-id :player-id, direction :direction} (key-to-player-direction key)
                player-id (if (nil? player-id) :player-1 player-id)
                arena (if (not (nil? direction)) (move arena player-id direction) arena)
                rows (arena-rows arena)
                {[x y] :coords} (player-id (:players arena))]
            (doseq [[row-idx row] (map-indexed vector rows)]
              (doseq [[char-idx char] (map-indexed vector row)]
                (s/put-string scr  ; screen
                              (if (= 0 char-idx) char-idx (* 2 char-idx))  ; x
                              row-idx  ; y
                              (if (nil? char) "." char)  ; string
                              {:fg (if (nil? char) :green :white)})))  ; options
            (s/move-cursor scr (* 2 x) y)
            (s/redraw scr)
            (recur arena (s/get-key-blocking scr))))))))