(ns bomberman-clj.domain.game-loop.frame-rate
  (:refer-clojure :exclude [set])
  (:require [clojure.core.async :as async]))

(defn set
  "Sends a refresh event to the given channel, `fps` times per second"
  [ch-out fps]
  (async/go-loop []
    (async/<! (async/timeout (/ 1000 fps)))
    (async/>! ch-out  {:type :refresh, :timestamp (System/currentTimeMillis)})
    (recur)))

(defn unset
  [ch-fps]
  (async/close! ch-fps))
