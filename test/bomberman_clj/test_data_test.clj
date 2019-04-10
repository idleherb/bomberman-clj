(ns bomberman-clj.test-data-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.test-data :refer [make-cell-bomb-p1
                                             make-cell-p1
                                             make-cell-wall
                                             make-player-1
                                             make-player-2
                                             make-player-3
                                             make-timestamp]]))

(facts "about test data"
  (fact "all data generation functions are spec compliant"
    (make-cell-p1) => #(specs/valid? ::specs/cell %)
    (make-cell-wall) => #(specs/valid? ::specs/cell %)
    (make-player-1) => #(specs/valid? ::specs/player %)
    (make-player-2) => #(specs/valid? ::specs/player %)
    (make-player-3) => #(specs/valid? ::specs/player %)
    (make-timestamp) => #(specs/valid? ::specs/timestamp %)))
