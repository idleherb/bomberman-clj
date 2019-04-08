(ns bomberman-clj.core
  (:require [clojure.core.async :as async]
            [bomberman-clj.arena :as arena]
            [bomberman-clj.specs :as specs]))

(defn- player-action
  [arena player-id action payload timestamp]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/player-id player-id)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/arena %)]}
  (condp = action
    :move (arena/move arena player-id payload)
    :plant-bomb (arena/plant-bomb arena player-id timestamp)
    (do
      (println "W - unknown player action:" action)
      arena)))

(defn game-loop
  [arena ch-in ch-out]
  {:pre [(specs/valid? ::specs/arena arena)
         (specs/valid? ::specs/chan ch-in)
         (specs/valid? ::specs/chan ch-out)]}
  (async/go-loop [arena arena]
    (if-let [event (async/<! ch-in)]
      (let [{timestamp :timestamp, type :type, :as event} event
            arena (if (= type :refresh)
              (let [arena (arena/eval-arena arena timestamp)]
                (async/>! ch-out {:state arena})
                arena)
              (player-action arena
                             (:player-id event)
                             (:action event)
                             (:payload event)
                             timestamp))]
        (recur arena))
      (println "D core::game-loop - game aborted..."))))
