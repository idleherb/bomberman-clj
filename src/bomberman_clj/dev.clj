(ns bomberman-clj.dev
  (:require [bomberman-clj.arena :refer :all])
  (:require [lanterna.screen :as s]))

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

(defn key-to-player-action
  [key]
  (case key
    :up {:player-id :player-1, :action #(move % :player-1 :north)}
    :right {:player-id :player-1, :action #(move % :player-1 :east)}
    :down {:player-id :player-1, :action #(move % :player-1 :south)}
    :left {:player-id :player-1, :action #(move % :player-1 :west)}
    \space {:player-id :player-1, :action #(plant-bomb % :player-1)}  ; fix call
    \w {:player-id :player-2, :action #(move % :player-2 :north)}
    \d {:player-id :player-2, :action #(move % :player-2 :east)}
    \s {:player-id :player-2, :action #(move % :player-2 :south)}
    \a {:player-id :player-2, :action #(move % :player-2 :west)}
    :tab {:player-id :player-2, :action #(plant-bomb % :player-2)}  ; fix call
    nil))

(defn dev-cell-player
  [cell]
  (cond
    (nil? cell) nil
    (not (map? cell)) (throw (Exception. (str "cell must be a map (was " (type cell) ")")))
    :else (let [players (filter (fn [[k v]] (re-matches #"player-\d+" (name k))) cell)]
      (first (map (fn [[_ v]] v) players)))))

(defn draw-arena
  [width height players]
  (let [scr (s/get-screen :swing)
        arena (init-arena width height players)]
    (s/in-screen scr
      (loop [arena arena
             key nil]
        (when (not= key :escape)
          (let [{player-id :player-id, action :action} (key-to-player-action key)
                player-id (if (nil? player-id) :player-1 player-id)
                arena (if (not (nil? action)) (action arena) arena)
                rows (arena-rows arena)
                [x y, :as coords] (player-id (:players arena))]
            (doseq [[row-idx row] (map-indexed vector rows)]
              (doseq [[cell-idx cell] (map-indexed vector row)]
                (s/put-string
                  scr  ; screen
                  (if (= 0 cell-idx) cell-idx (* 2 cell-idx))  ; x
                  row-idx  ; y
                  (let [player (dev-cell-player cell)]
                    (when (not (nil? cell)) (print cell " "))
                    (cond
                      (nil? cell) "."
                      (not (nil? player)) (str (:glyph player))
                      (contains? cell :bomb) "X"
                      :else (throw (Exception. (str "invalid cell content: " cell))))
                  )  ; string
                  {:fg (if (nil? cell) :green :white),
                    :bg (if (contains? cell :bomb) :red :black)})))  ; options
            (s/move-cursor scr (* 2 x) y)
            (s/redraw scr)
            (recur arena (s/get-key-blocking scr))))))))