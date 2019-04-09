(ns bomberman-clj.core
  (:require [clojure.core.async :as async]
            [bomberman-clj.arena :as arena]
            [bomberman-clj.dev-client :as dev-client]
            [bomberman-clj.frame-rate :as fps]
            [bomberman-clj.specs :as specs]))

(defn- player-action
  [arena event]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/event event)]
   :post [(specs/valid? ::specs/arena %)]}
  (let [{:keys [timestamp player-id action payload]} event]
    (condp = action
      :move (arena/move arena player-id payload)
      :plant-bomb (arena/plant-bomb arena player-id timestamp)
      (do
        (println "W - unknown player action:" action)
        arena))))

(defn game-loop
  [arena ch-in ch-out]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/chan ch-in)
         (specs/valid? ::specs/chan ch-out)]
   :post [(specs/valid? ::specs/chan %)]}
  (async/go-loop [arena arena]
    (if-let [{timestamp :timestamp, type :type, :as event} (async/<! ch-in)]
      (if (= type :exit)
        (do
          (println "W core::game-loop - exit...")
          event)
        (let [arena (condp = type
                      :refresh (let [arena (arena/eval-arena arena timestamp)]
                                 (async/>! ch-out {:state arena, :timestamp timestamp})
                                 arena)
                      :action (player-action arena event)
                      (do
                        (println "W core::game-loop - unexpected event:" event)
                        arena))]
          (recur arena)))
      (println "W core::game-loop - game aborted..."))))

(defn main
  []
  (let [arena (arena/init-arena 25 15 {:player-1 {:glyph \@}
                                       :player-2 {:glyph \&}})
        ch-in (async/chan)
        ch-out (async/chan)
        ch-game (game-loop arena ch-in ch-out)
        _ (future (dev-client/join arena ch-in ch-out))
        ch-fps (fps/set ch-in 30)]
      (if-let [event (async/<!! ch-game)]
        (if (= (:type event) :exit)
          (do
            (println "I core::main - client left the game...")
            (fps/unset ch-fps)
            (async/close! ch-game)
            (async/close! ch-out)
            (async/close! ch-in)
            (println "I core::main - exit."))
          (println "E core::main - invalid event:" event))
        (println "W core::main - aborted."))))
