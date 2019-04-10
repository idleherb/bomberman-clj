(ns bomberman-clj.grid-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.grid :refer [cell-at
                                        cell-empty?
                                        find-empty-cell
                                        rand-coords
                                        spawn]]
            [bomberman-clj.test-data :refer [make-cell-bomb-p1
                                             make-cell-wall
                                             make-player-1
                                             make-player-2
                                             make-player-3
                                             make-timestamp]]))

(facts "about grids"
  (fact "3 players spawn at the given positions"
    (let [width 17
          height 15
          grid {:width width
                :height height
                :v (into (vector) (take (* width height) (repeat nil)))}
          grid (spawn grid :player-1 (make-player-1) {:x 0, :y 0})
          grid (spawn grid :player-2 (make-player-2) {:x 12, :y 7})
          grid (spawn grid :player-3 (make-player-3) {:x 16, :y 14})
          cell-player-1 (nth (:v grid) 0)
          cell-player-2 (nth (:v grid) 131)
          cell-player-3 (nth (:v grid) 254)]
      (count cell-player-1) => 1
      (:player-1 cell-player-1) => (make-player-1)
      (count cell-player-2) => 1
      (:player-2 cell-player-2) => (make-player-2)
      (count cell-player-3) => 1
      (:player-3 cell-player-3) => (make-player-3)))

  (fact "cells can being identified by their :x and :y coordinates"
    (let [bmb (make-cell-bomb-p1)
          wal (make-cell-wall)
          width 2
          height 3
          grid {:width width
                :height height
                :v [bmb bmb
                    nil wal
                    nil bmb]}]
      (cell-at grid {:x 0, :y 0}) => bmb
      (cell-at grid {:x 1, :y 0}) => bmb
      (cell-empty? grid {:x 0, :y 1}) => true
      (cell-at grid {:x 1, :y 1}) => wal
      (cell-empty? grid {:x 0, :y 2}) => true
      (cell-at grid {:x 1, :y 2}) => bmb))

  (fact "a pair of random coordinates is being found"
    (let [width 2
          height 3
          grid {:width width
                :height height
                :v [nil nil
                    nil nil
                    nil nil]}
          {:keys [x y]} (rand-coords grid)]
      (< x width) => true
      (< y height) => true))

  (fact "an empty cell is being found"
    (let [wal (make-cell-wall)
          grid {:width 3
                :height 3
                :v [wal wal wal
                    wal wal wal
                    wal nil wal]}]
      (find-empty-cell grid) => {:x 1, :y 2})))
