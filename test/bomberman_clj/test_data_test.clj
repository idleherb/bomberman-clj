(ns bomberman-clj.test-data-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.specs :as s]
            [bomberman-clj.test-data :as d]))

(facts "about test data"
  (fact "all data generation functions are spec compliant"
    (d/make-cell-bomb-p1) => #(s/valid? ::s/cell %)
    (d/make-cell-p1) => #(s/valid? ::s/cell %)
    (d/make-cell-p2) => #(s/valid? ::s/cell %)
    (d/make-cell-hard-block) => #(s/valid? ::s/cell %)
    (d/make-cell-soft-block) => #(s/valid? ::s/cell %)
    (d/make-cell-item-bomb) => #(s/valid? ::s/cell %)
    (d/make-player-1) => #(s/valid? ::s/player %)
    (d/make-player-2) => #(s/valid? ::s/player %)
    (d/make-player-3) => #(s/valid? ::s/player %)
    (d/make-timestamp) => #(s/valid? ::s/timestamp %))
)
