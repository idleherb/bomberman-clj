(ns bomberman-clj.frame-rate
  (:require [clojure.core.async :as async]
            [bomberman-clj.specs :as specs]))

(defn set
  "Sends a refresh event to the given channel, `fps` times per second"
  [ch-out fps]
  {:pre [(specs/valid? ::specs/chan ch-out)]
   :post [(specs/valid? ::specs/chan %)]}
  (async/go-loop []
    (async/<! (async/timeout (/ 3600 fps)))
    (async/>! ch-out  {:type :refresh, :timestamp (System/currentTimeMillis)})
    (recur)))

(defn unset
  [ch-frame-rate]
  {:pre [(specs/valid? ::specs/chan ch-frame-rate)]}
  (async/close! ch-frame-rate))
