(ns bomberman-clj.util-test
  (:require [midje.sweet :refer [fact facts => throws]]
            [bomberman-clj.util :refer [expired?
                                        navigate]]
            [bomberman-clj.test-data :refer [make-timestamp]]))

(facts "about util"
  (facts "about navigating"
    (fact "works as expected when using valid directions"
      (let [{:keys [x y]} (
            -> {:x 1, :y 1}
              (navigate :down)
              (navigate :left)
              (navigate :up)
              (navigate :up)
              (navigate :right))]
        x => 1
        y => 0))

    (fact "does nothing when using an invalid direction"
      (navigate {:x 0, :y 0} :south) => {:x 0, :y 0}))
      
  (facts "about expired timetamps"
    (fact "timestamp expiration"
      (let [ts-old (make-timestamp)
            ts-new (+ 200 ts-old)]
        (expired? ts-old ts-new 200) => true
        (expired? ts-old ts-new 100) => true
        (expired? ts-old ts-new 300) => false
        (expired? ts-new ts-old 200) => false)))
)
