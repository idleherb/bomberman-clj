(ns bomberman-clj.util-test
  (:require [midje.sweet :refer [fact facts => throws]]
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
      
  (facts "about expired timetamps"
    (fact "timestamp expiration"
      (let [ts-old (d/make-timestamp)
            ts-new (+ 200 ts-old)]
        (u/expired? ts-old ts-new 200) => true
        (u/expired? ts-old ts-new 100) => true
        (u/expired? ts-old ts-new 300) => false
        (u/expired? ts-new ts-old 200) => false)))
)
