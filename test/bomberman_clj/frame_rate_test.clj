(ns bomberman-clj.frame-rate-test
  (:require [clojure.core.async :as async]
            [midje.sweet :refer [fact facts => roughly]]
            [bomberman-clj.frame-rate :as fr]))

(facts "about the frame rate"
  (fact "sends a refresh event roughly `fps` times per second"
    (let [ch (async/chan)
          ch-fr (fr/set ch 30)
          events (loop [events [] i 0]
                   (if (= i 30)
                     events
                     (recur (conj events (async/<!! ch)) (inc i))))
          _ (fr/unset ch-fr)]
      (count events) => 30
      (let [ts-first (:timestamp (first events))
            ts-last (:timestamp (last events))]
        (- ts-last ts-first) => (roughly 1000 100))))
)
