(ns bomberman-clj.domain.game-loop.frame-rate
  (:refer-clojure :exclude [set])
  (:require [clojure.core.async :as a]))

(defn set
  "Sends a refresh event to the given channel, `fps` times per second"
  [ch-out fps]
  (a/go-loop []
    (a/<! (a/timeout (/ 1000 fps)))
    (a/>! ch-out  {:type :refresh, :timestamp (System/currentTimeMillis)})
    (recur)))

(defn unset
  [ch-fps]
  (a/close! ch-fps))
