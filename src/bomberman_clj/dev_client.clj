(ns bomberman-clj.dev-client
  (:require [clojure.core.async :as async]
            [bomberman-clj.game :as game]
            [bomberman-clj.config :as config]
            [bomberman-clj.grid :as grid]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.util :as util]
            [lanterna.screen :as s]))

(def h-margin 20)
(def v-margin 10)

(defn- grid-rows
  [game]
  (let [grid (:grid game)
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
    (s/put-string scr x y text {:fg :white, :bg :black})))

(defn- draw-game
  [game scr]
  (let [{{:keys [width height], :as grid} :grid, players :players} game
        gameover (:gameover game)]
    (clear-screen scr (+ (* 2 h-margin) (* 2 width)) (+ (* 2 v-margin) height))
    (if (and (some? gameover))
      (let [text (if-let [player-id (:winner gameover)]
              (let [player (get players player-id)]
                (str "*** " (:name player) " wins! ***"))
              "*** No winner! ***")]
        (draw-centered-text scr width height text))
      ; else
      (do
        (doseq [[row-idx row] (map-indexed vector (grid-rows game))]
          (doseq [[cell-idx cell] (map-indexed vector row)]
            (let [item (grid/cell-item cell)
                  player (game/cell-player game cell)
                  block? (grid/block? cell)
                  hard-block? (grid/hard-block? cell)
                  soft-block? (grid/soft-block? cell)
                  bomb? (grid/bomb? cell)
                  fire? (grid/fire? cell)]
              (when (some? player)
                (let [player-idx (Integer/parseInt (second (re-matches #".*?(\d+)" (name (:player-id player)))))
                      x (+ h-margin (* 2 cell-idx))
                      y (if (< row-idx (/ height 2))
                          (- v-margin 2 (- (count players) player-idx))
                          (+ v-margin height player-idx))
                      text (str (:name player) " "
                                (:bomb-count player) "/" (:bomb-radius player))]
                  (s/put-string scr x y text {:fg :black, :bg :white})))
              (s/put-string
                scr  ; screen
                (+ h-margin (* 2 cell-idx))  ; x
                (+ v-margin row-idx)  ; y
                (cond
                  (nil? cell) "."
                  (some? player) (str (:glyph player))
                  bomb? "X"
                  hard-block? (str (:hard (:block config/glyphs)))
                  soft-block? (str (:soft (:block config/glyphs)))
                  (some? item) (str ((:type item) (:item config/glyphs)))
                  fire? "#"
                  :else (throw (Exception. (str "invalid cell content: " cell))))  ; string
                {:fg (cond
                       (nil? cell) :green
                       (and soft-block? fire?) :yellow
                       block? :green
                       fire? :black
                       (some? item) :white
                       (some? player) :black
                       :else :white)
                 :bg (cond
                       (and soft-block? fire?) :black
                       fire? :yellow
                       bomb? :red
                       (some? item) :blue
                       (some? player) :white
                       :else :black)}))))  ; options
        (s/move-cursor scr 100 100)))
    (s/redraw scr)))

(defn- key-to-event
  [key]
  (let [timestamp (System/currentTimeMillis)]
    (case key
      :up {:type :action, :payload {:player-id :player-1, :action :move, :direction :up}, :timestamp timestamp}
      :right {:type :action, :payload {:player-id :player-1, :action :move, :direction :right}, :timestamp timestamp}
      :down {:type :action, :payload {:player-id :player-1, :action :move, :direction :down}, :timestamp timestamp}
      :left {:type :action, :payload {:player-id :player-1, :action :move, :direction :left}, :timestamp timestamp}
      \space {:type :action, :payload {:player-id :player-1, :action :plant-bomb}, :timestamp timestamp}
      \w {:type :action, :payload {:player-id :player-2, :action :move, :direction :up}, :timestamp timestamp}
      \d {:type :action, :payload {:player-id :player-2, :action :move, :direction :right}, :timestamp timestamp}
      \s {:type :action, :payload {:player-id :player-2, :action :move, :direction :down}, :timestamp timestamp}
      \a {:type :action, :payload {:player-id :player-2, :action :move, :direction :left}, :timestamp timestamp}
      :tab {:type :action, :payload {:player-id :player-2, :action :plant-bomb}, :timestamp timestamp}
      :enter {:type :restart, :timestamp timestamp}
      {:type :dummy})))

(defn join
  [ch-in ch-out player-1-name player-2-name width height]
  {:pre [(specs/valid? ::specs/chan ch-in)
         (specs/valid? ::specs/chan ch-out)]
   :post [(specs/valid? ::specs/chan %)]}
  (let [scr (s/get-screen :swing {:rows (+ (* 2 v-margin) height)
                                  :cols (+ (* 2 h-margin) (* 2 width))})]
    (s/in-screen scr
      (async/go-loop []
        (if-let [{game :payload, :as event} (async/<! ch-out)]
          (do
            (draw-game game scr)
            (recur))
          (println "D dev_client::join - 0 fps.")))
      (async/>!! ch-in {:type :join, :payload {:name player-1-name}})
      (async/>!! ch-in {:type :join, :payload {:name player-2-name}})
      (loop []
        (let [key (s/get-key-blocking scr)]
          (condp = key
            :escape (do
              (println "D dev_client::join - exit requested...")
              (async/go (async/>! ch-in {:type :exit, :timestamp (System/currentTimeMillis)})))
            (do
              (async/go (async/>! ch-in (key-to-event key)))
              (recur))))))))
