(ns bomberman-clj.util-test
  (:require [midje.sweet :refer [fact facts => throws]]
            [bomberman-clj.util :refer [navigate]]))

(facts "about util"
  (fact "navigating just works as expected"
    (let [coords [1 1]
          coords (navigate coords :south)
          coords (navigate coords :west)
          coords (navigate coords :north)
          coords (navigate coords :north)
          [x y] coords]
      x => 0
      y => 0))

  (fact "navigating into an invalid direction fails"
    (navigate [0 0] :down) => (throws Exception #"invalid direction:" )))
