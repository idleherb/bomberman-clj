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
          grid (spawn grid :player-1 {:glyph \P, :bomb-count 3} {:x 0, :y 0})
          grid (spawn grid :player-2 {:glyph \Q, :bomb-count 3} {:x 12, :y 7})
          grid (spawn grid :player-3 {:glyph \R, :bomb-count 3} {:x 16, :y 14})
          cell-player-1 (nth (:v grid) 0)
          cell-player-2 (nth (:v grid) 131)
          cell-player-3 (nth (:v grid) 254)]
      (count cell-player-1) => 1
      (:player-1 cell-player-1) => {:glyph \P, :bomb-count 3}
      (count cell-player-2) => 1
      (:player-2 cell-player-2) => {:glyph \Q, :bomb-count 3}
      (count cell-player-3) => 1
      (:player-3 cell-player-3) => {:glyph \R, :bomb-count 3}))

  (fact "cells are being identified correctly"
    (let [bmb {:bomb {:timestamp (make-timestamp)}}
          width 2
          height 3
          grid {:width width
                :height height
                :v [bmb bmb
                    nil bmb
                    nil bmb]}]
      (cell-at grid {:x 0, :y 0}) => bmb
      (cell-at grid {:x 1, :y 0}) => bmb
      (cell-empty? grid {:x 0, :y 1}) => true
      (cell-at grid {:x 1, :y 1}) => bmb
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
          {:keys [x y], :as coords} (rand-coords grid)]
      (< x width) => true
      (< y height) => true))

  (fact "an empty cell is being found"
    (let [plr {:glyph \P, :bomb-count 3}
          width 3
          height 3
          grid {:width width
                :height height
                :v [{:player-1 plr} {:player-2 plr} {:player-3 plr}
                    {:player-4 plr} {:player-5 plr} {:player-6 plr}
                    {:player-7 plr} nil             {:player-8 plr}]}]
        (find-empty-cell grid) => {:x 1, :y 2})))
