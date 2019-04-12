(ns bomberman-clj.util-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.util :as u]
            [bomberman-clj.test-data :as d]))

(facts "about util"
  (facts "about navigating"
    (fact "works as expected when using valid directions"
      (let [{:keys [x y]} (
            -> {:x 1, :y 1}
              (u/navigate :down)
              (u/navigate :left)
              (u/navigate :up)
              (u/navigate :up)
              (u/navigate :right))]
        x => 1
        y => 0))

    (fact "does nothing when using an invalid direction"
      (u/navigate {:x 0, :y 0} :south) => {:x 0, :y 0}))
)
