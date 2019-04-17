(ns bomberman-clj.core
  (:require [clojure.core.async :as async]
            [bomberman-clj.config :as config]
            [bomberman-clj.dev-client :as dc]
            [bomberman-clj.frame-rate :as fps]
            [bomberman-clj.game-loop :as gl])
  (:gen-class))

(defn main
  ([width height player-1-name player-2-name]
    (let [ch-in (async/chan)
          ch-out (async/chan)
          ch-game (gl/game-loop ch-in ch-out 2 width height)
          _ (future (dc/join ch-in ch-out player-1-name player-2-name width height))
          ch-fps (fps/set ch-in config/fps)]
        (if-let [event (async/<!! ch-game)]
          (if (= (:type event) :exit)
            (do
              (println "I core::main - client requested game exit...")
              (fps/unset ch-fps)
              (async/close! ch-game)
              (async/close! ch-out)
              (async/close! ch-in)
              (println "I core::main - exit."))
            (println "E core::main - invalid event:" event))
          (println "W core::main - aborted."))))
  ([] (main config/arena-width config/arena-height)))

(defn -main
  [width height player-1-name player-2-name]
  (main (Integer/parseInt width) (Integer/parseInt height) player-1-name player-2-name)
  (System/exit 0))

