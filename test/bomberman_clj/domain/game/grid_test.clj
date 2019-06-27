(ns bomberman-clj.domain.game.grid-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.domain.game.grid :as g]
            [bomberman-clj.domain.game.test-data :as d]))

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
  
  (fact "a player can be spawned in a full grid"
    (let [hbl (d/make-cell-hard-block)
          sbl (d/make-cell-soft-block)
          plr (d/make-player :player-1)
          [grid plr] (-> {:width 3
                          :height 3
                          :v [hbl sbl hbl
                              sbl sbl sbl
                              hbl sbl hbl]}
                       (g/spawn-player plr))
          v (:v grid)
          num_blocks (count (filter #(:block %) v))
          num_player (count (filter #(:player-id %) v))
          num_empty (count (filter nil? v))]
      (:coords plr) => some?
      num_blocks => 6
      num_empty => 2
      num_player => 1))
)
