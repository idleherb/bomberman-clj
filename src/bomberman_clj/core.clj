(ns bomberman-clj.core
  (:require [clojure.core.async :as async]
            [bomberman-clj.config :as config]
            [bomberman-clj.dev-client :as dc]
            [bomberman-clj.frame-rate :as fps]
            [bomberman-clj.game-loop :as gl]
            [bomberman-clj.server.server :as server])
  (:gen-class))

(defn main
  ([width height num-players host port]
    (let [ch-in (async/chan)
          ch-out (async/chan)
          ch-game (gl/game-loop ch-in ch-out num-players width height)
          _ (future (server/run ch-in
                                ch-out
                                num-players
                                host
                                port))
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
  ([width height num-players host port]
    (main (Integer/parseInt width)
          (Integer/parseInt height)
          (Integer/parseInt num-players)
          host
          port)
    (System/exit 0))
  ([]
    (-main "17" "15" "2" "0.0.0.0" "8080")))
