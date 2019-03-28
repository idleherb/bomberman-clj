(ns bomberman-clj.grid-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.grid :refer [cell-at
                                        cell-empty?
                                        find-empty-cell
                                        rand-coords
                                        spawn]]
            [bomberman-clj.test-data :refer [make-timestamp]]))

(facts "about grids"
  (fact "3 players should spawn in the correct positions"
    (let [width 17
          height 15
          grid {:width width, :height height, :v (into (vector) (take (* width height) (repeat nil)))}
          grid (spawn grid :player-1 {:glyph \P} [0 0])
          grid (spawn grid :player-2 {:glyph \Q} [12 7])
          grid (spawn grid :player-3 {:glyph \R} [16 14])
          cell-player-1 (nth (:v grid) 0)
          cell-player-2 (nth (:v grid) 131)
          cell-player-3 (nth (:v grid) 254)]
      (count cell-player-1) => 1
      (:player-1 cell-player-1) => {:glyph \P}
      (count cell-player-2) => 1
      (:player-2 cell-player-2) => {:glyph \Q}
      (count cell-player-3) => 1
      (:player-3 cell-player-3) => {:glyph \R}))

  (fact "cells are being identified correctly"
    (let [bmb {:bomb {:timestamp (make-timestamp)}}
          width 2
          height 3
          grid {:width width
                :height height
                :v [bmb bmb
                    nil bmb
                    nil bmb]}]
      (cell-at grid [0 0]) => bmb
      (cell-at grid [1 0]) => bmb
      (cell-empty? grid [0 1]) => true
      (cell-at grid [1 1]) => bmb
      (cell-empty? grid [0 2]) => true
      (cell-at grid [1 2]) => bmb))

  (fact "a pair of random coordinates is being found"
    (let [width 2
          height 3
          grid {:width width
                :height height
                :v [nil nil
                    nil nil
                    nil nil]}
          [x y] (rand-coords grid)]
      x => #(< % width)
      y => #(< % height)))

  (fact "an empty cell is being found"
    (let [plr {:glyph \P}
          width 3
          height 3
          grid {:width width
                :height height
                :v [{:player-1 plr} {:player-2 plr} {:player-3 plr}
                    {:player-4 plr} {:player-5 plr} {:player-6 plr}
                    {:player-7 plr} nil             {:player-8 plr}]}]
        (find-empty-cell grid) => [1 2])))
