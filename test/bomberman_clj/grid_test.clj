(ns bomberman-clj.grid-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.grid :as g]
            [bomberman-clj.test-data :as d]))

(facts "about grids"

  (fact "cells can being identified by their :x and :y coordinates"
    (let [bmb (d/make-cell-bomb-p1)
          wal (d/make-cell-hard-block)
          width 2
          height 3
          grid {:width width
                :height height
                :v [bmb bmb
                    nil wal
                    nil bmb]}]
      (g/cell-at grid {:x 0, :y 0}) => bmb
      (g/cell-at grid {:x 1, :y 0}) => bmb
      (g/cell-empty? grid {:x 0, :y 1}) => true
      (g/cell-at grid {:x 1, :y 1}) => wal
      (g/cell-empty? grid {:x 0, :y 2}) => true
      (g/cell-at grid {:x 1, :y 2}) => bmb))

  (fact "an empty cell is being found"
    (let [wal (d/make-cell-hard-block)
          grid {:width 3
                :height 3
                :v [wal wal wal
                    wal wal wal
                    wal nil wal]}]
      (g/find-empty-cell grid) => {:x 1, :y 2}))
)
