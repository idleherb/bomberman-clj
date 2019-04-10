(ns bomberman-clj.dev-client
  (:require [clojure.core.async :as async]
            [bomberman-clj.arena :as arena]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.specs :as specs]
            [lanterna.screen :as s]))

(def h-margin 20)
(def v-margin 10)

(defn- arena-rows
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

(defn- clear-screen
  [scr width height]
  (let [mask (reduce str (repeat width " "))]
    (loop [y 0]
      (when (< y height)
        (do
          (s/put-string scr 0 y mask {:bg :black})
          (recur (inc y)))))))

(defn- draw-centered-text
  [scr width height text]
  (let [x (+ h-margin (int (/ (- (* 2 width) (count text)) 2)))
        y (+ v-margin (int (/ height 2)))]
    (clear-screen scr (+ (* 2 h-margin) (* 2 width)) (+ (* 2 v-margin) height))
    (s/put-string scr x y text {:fg :white, :bg :black})))

(defn- draw-arena
  [arena scr]
  (if-let [gameover (:gameover arena)]
    (let [{{:keys [width height], :as grid} :grid} arena
          text (if (contains? gameover :winner)
                 (str "*** " (:winner gameover) " won! ***")
                 "*** No winner! ***")]
      (draw-centered-text scr width height text))
    ; else
    (do
      (doseq [[row-idx row] (map-indexed vector (arena-rows arena))]
        (doseq [[cell-idx cell] (map-indexed vector row)]
          (let [bomb (cells/cell-bomb cell)
                fire? (contains? cell :fire)
                player (cells/cell-player cell)]
            (s/put-string
              scr  ; screen
              (+ h-margin (if (= 0 cell-idx) cell-idx (* 2 cell-idx)))  ; x
              (+ v-margin row-idx)  ; y
                (cond
                  (nil? cell) "."
                  (some? player) (str (:glyph player))
                  (some? bomb) "X"
                  fire? "#"
                  :else (throw (Exception. (str "invalid cell content: " cell))))  ; string
              {:fg (cond
                    (nil? cell) :green
                    fire? :black
                    :else :white)
              :bg (cond
                    fire? :yellow
                    (some? bomb) :red
                    :else :black)}))))  ; options
      (s/move-cursor scr 100 100)))
  (s/redraw scr))

(defn- key-to-event
  [key]
  (let [timestamp (System/currentTimeMillis)]
    (case key
      :up {:type :action, :player-id :player-1, :action :move, :payload :up, :timestamp timestamp}
      :right {:type :action, :player-id :player-1, :action :move, :payload :right, :timestamp timestamp}
      :down {:type :action, :player-id :player-1, :action :move, :payload :down, :timestamp timestamp}
      :left {:type :action, :player-id :player-1, :action :move, :payload :left, :timestamp timestamp}
      \space {:type :action, :player-id :player-1, :action :plant-bomb, :timestamp timestamp}
      \w {:type :action, :player-id :player-2, :action :move, :payload :up, :timestamp timestamp}
      \d {:type :action, :player-id :player-2, :action :move, :payload :right, :timestamp timestamp}
      \s {:type :action, :player-id :player-2, :action :move, :payload :down, :timestamp timestamp}
      \a {:type :action, :player-id :player-2, :action :move, :payload :left, :timestamp timestamp}
      :tab {:type :action, :player-id :player-2, :action :plant-bomb, :timestamp timestamp}
      {:type :dummy})))

(defn join
  [arena ch-in ch-out]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/chan ch-in)
         (specs/valid? ::specs/chan ch-out)]
   :post [(specs/valid? ::specs/chan %)]}
  (let [{{:keys [width height] :as grid} :grid} arena
        scr (s/get-screen :swing {:rows (+ (* 2 v-margin) height)
                                  :cols (+ (* 2 h-margin) (* 2 width))})]
    (s/in-screen scr
      (async/go-loop []
        (if-let [{arena :state, :as event} (async/<! ch-out)]
          (do
            (draw-arena arena scr)
            (recur))
          (println "D dev_client::join - 0 fps.")))
      (loop []
        (let [key (s/get-key-blocking scr)]
          (if (= key :escape)
            (do
              (println "D dev_client::join - exit requested...")
              (async/go (async/>! ch-in {:type :exit})))
            (do
              (async/go (async/>! ch-in (key-to-event key)))
              (recur))))))))
