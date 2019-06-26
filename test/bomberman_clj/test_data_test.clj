(ns bomberman-clj.test-data-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.specs :as s]
            [bomberman-clj.test-data :as d]))

(facts "about test data"
  (fact "all data generation functions are spec compliant"
    (d/make-cell-bomb-p1) => #(s/valid? ::s/cell %)
    (d/make-cell-hard-block) => #(s/valid? ::s/cell %)
    (d/make-cell-soft-block) => #(s/valid? ::s/cell %)
    (d/make-cell-item-bomb) => #(s/valid? ::s/cell %)
    (d/make-cell-item-rc) => #(s/valid? ::s/cell %)
    (d/make-cell-item-bk) => #(s/valid? ::s/cell %)
    (d/make-timestamp) => #(s/valid? ::s/timestamp %)
    (d/make-player :player-1) => #(s/valid? ::s/player %)
    (d/make-player :player-1 {:x 0, :y 0}) => #(s/valid? ::s/player %)
    (d/make-empty-game) => #(s/valid? ::s/game %)
    (d/make-game (d/make-timestamp)) => #(s/valid? ::s/game %)))
