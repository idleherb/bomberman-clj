(ns bomberman-clj.arena-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.arena :as a]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))

(facts "about arenas"
  (fact "initializing an arena of size 17x15 with 2 players and 0 bombs"
    (let [width 17
          height 15
          players {:player-1 {:glyph \P}, :player-2 {:glyph \Q}}
          arena (a/init width height players)
          {{v :v, :as grid} :grid, players :players, bombs :bombs} arena]
      (count v) => (* width height)
      (count (filter (complement nil?) v)) => #(>= % (+ 2 (* 8 7)))
      (count players) => 2
      (count bombs) => 0))

  (fact "a player can move to NESW empty cells"
    (let [plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          arena (a/move arena :player-1 :down)
          arena (a/move arena :player-1 :down)  ; hit block
          arena (a/move arena :player-1 :left)
          arena (a/move arena :player-1 :left)  ; hit block
          arena (a/move arena :player-1 :left)  ; hit block
          arena (a/move arena :player-1 :up)
          arena (a/move arena :player-1 :up)
          arena (a/move arena :player-1 :up)  ; hit block
          arena (a/move arena :player-1 :up)  ; hit block
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 0
      y => 0
      (nth v 4) => nil?))

  (fact "a player can't move through solid blocks"
    (let [plr {:player-1 {:glyph \P, :bomb-count 3}}
          hbl {:block :hard}
          v [nil nil nil
             hbl plr nil
             nil hbl nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          arena (a/move arena :player-1 :down)  ; hit block
          arena (a/move arena :player-1 :down)  ; hit block
          arena (a/move arena :player-1 :left)  ; hit block
          arena (a/move arena :player-1 :left)  ; hit block
          arena (a/move arena :player-1 :left)  ; hit block
          arena (a/move arena :player-1 :up)
          arena (a/move arena :player-1 :up)  ; hit block
          arena (a/move arena :player-1 :up)  ; hit block
          arena (a/move arena :player-1 :up)  ; hit block
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 1
      y => 0
      (nth v 4) => nil?))

  (fact "player-1 places a bomb at their current position"
    (let [timestamp (d/make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 3}}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (count bombs) => 1
      (let [bomb (:bomb-x0y0 bombs)]
        bomb => map?
        (count bomb) => 2)
      (:timestamp (:bomb-x0y0 cell)) => timestamp))

  (fact "in one cell, only one bomb can be planted at any moment in time"
    (let [ts-1st 1000000000000
          ts-2nd 2000000000000
          v [{:player-1 {:glyph \P, :bomb-count 3}}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (a/plant-bomb arena :player-1 ts-1st)
          arena (a/plant-bomb arena :player-1 ts-2nd)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (count bombs) => 1
      (let [bomb (:bomb-x0y0 bombs)]
        bomb => map?
        (count bomb) => 2)
      (:timestamp (:bomb-x0y0 cell)) => ts-1st))

  (fact "a planted bomb is still there after the player moves away"
    (let [timestamp (d/make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 3}} nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} (a/move arena :player-1 :down)
          cell (first v)]
      (count bombs) => 1
      (:timestamp (:bomb-x0y0 cell)) => timestamp))

  (fact "a player can't plant more bombs than they have"
    (let [timestamp (d/make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 2}} nil nil nil]
          arena {:grid {:width 1, :height 4, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down)
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down)
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down)
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (nth v 3)
          player (:player-1 cell)]
      (count bombs) => 2
      (cells/cell-bomb (first v)) => (complement nil?)
      (cells/cell-bomb (second v)) => (complement nil?)
      (cells/cell-bomb (nth v 2)) => nil?
      (cells/cell-bomb (nth v 3)) => nil?
      (:bomb-count player) => 0))

  (fact "an evaluated arena without any bombs has no changes"
    (let [pl1 {:player-1 {:glyph \P, :bomb-count 1}}
          pl2 {:player-2 {:glyph \Q, :bomb-count 1}}
          v [nil nil pl2
             nil pl1 nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 0, :y 2}}
                 :bombs {}}]
          (a/eval arena (d/make-timestamp)) => arena))

  (fact "an evaluated arena with an expired bomb contains fire"
    (let [timestamp (d/make-timestamp)
          hbl (d/make-cell-hard-block)
          sbl (d/make-cell-soft-block)
          bomb-id :bomb-x0y0
          bom {bomb-id {:player-id :player-1, :timestamp 1000000000000}}
          plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [bom hbl nil
             nil plr nil
             sbl nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}
                 :bombs {bomb-id {:x 0, :y 0}}}
          {{v :v, :as grid} :grid
            bombs :bombs
            :as evaluated-arena} (a/eval arena timestamp)]
      evaluated-arena =not=> arena
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp timestamp})
          ?cell
          (nth v 0)
          (nth v 3)
          (nth v 6))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 1)
          (nth v 2)
          (nth v 4)
          (nth v 5)
          (nth v 7)
          (nth v 8))
      (count bombs) => 1
      (let [bomb (bomb-id (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player-1 (nth v 4))) => nil?
      (:hit (:block (nth v 6))) => {:timestamp timestamp}))

  (fact "an evaluated arena with an expired bomb hits the nearby player"
    (let [timestamp (d/make-timestamp)
          bomb-id1 :bomb-x0y0
          bomb-id2 :bomb-x2y1
          bm1 {bomb-id1 {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {bomb-id2 {:player-id :player-1, :timestamp 1000000000000}}
          plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [bm1 nil nil
             nil nil bm2
             plr nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 0, :y 2}}
                 :bombs {bomb-id1 {:x 0, :y 0}
                         bomb-id2 {:x 2, :y 1}}}
          {{v :v, :as grid} :grid
            bombs :bombs
            :as evaluated-arena} (a/eval arena timestamp)]
      evaluated-arena =not=> arena
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp timestamp})
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2)
          (nth v 3)
          (nth v 4)
          (nth v 6)
          (nth v 5)
          (nth v 8))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 7))
      (count bombs) => 2
      (let [bomb (bomb-id1 (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (let [bomb (bomb-id2 (nth v 5))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player-1 (nth v 6))) => {:timestamp timestamp}))

  (fact "bomb detonations propagate to nearby bombs, leaving others unchanged"
    (let [bm1 {:bomb-x0y0 {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {:bomb-x2y0 {:player-id :player-1, :timestamp 9999999999999}}
          bm3 {:bomb-x1y2 {:player-id :player-1, :timestamp 9999999999999}}
          plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [bm1 nil bm2
             nil plr nil
             nil bm3 nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}
                 :bombs {:bomb-x0y0 {:x 0, :y 0}
                         :bomb-x2y0 {:x 2, :y 0}
                         :bomb-x1y2 {:x 1, :y 2}}}
          {{v :v, :as grid} :grid
            bombs :bombs
            :as evaluated-arena} (a/eval arena 2222222222222)]
      evaluated-arena =not=> arena
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp 2222222222222})
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2)
          (nth v 3)
          (nth v 5)
          (nth v 6)
          (nth v 8))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 4)
          (nth v 7))
      (let [bomb (:bomb-x0y0 (nth v 0))]
        (:detonated bomb) => {:timestamp 2222222222222})
      (let [bomb (:bomb-x2y0 (nth v 2))]
        (:detonated bomb) => {:timestamp 2222222222222})
      (let [bomb (:bomb-x1y2 (nth v 7))]
        (:detonated bomb) => nil?)
      (:hit (:player-1 (nth v 4))) => nil?
      (count bombs) => 3))

  (fact "a player hblking into a cell with fire gets hit"
    (let [timestamp 1000000003022
          bom {:bomb-x0y0 {:player-id :player-1, :timestamp 1000000000000}}
          pl1 {:player-1 {:glyph \P, :bomb-count 3}}
          pl2 {:player-2 {:glyph \Q, :bomb-count 3}}
          v [bom nil nil
             nil pl1 pl2]
          arena {:grid {:width 3, :height 2, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 2, :y 1}}
                 :bombs {:bomb-x0y0 {:x 0, :y 0}}}
          arena (a/eval arena 1000000003000)
          arena (a/move arena :player-1 :up)
          arena (a/eval arena timestamp)
          {{v :v, :as grid} :grid, :as arena} arena]
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp 1000000003000})
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 4)
          (nth v 5))
      (let [bomb (:bomb-x0y0 (nth v 0))]
        (:detonated bomb) => {:timestamp 1000000003000})
      (:hit (:player-1 (nth v 1))) => {:timestamp timestamp}))
     
  (fact "at arena evaluation, expired detonated bombs, fire and hit players get removed"
    (let [bomb {:player-id :player-1
                :timestamp 1000000000000
                :detonated {:timestamp 1000000010000}}
          player {:glyph \P
                  :hit {:timestamp 1000000010000}
                  :bomb-count 0}
          v [{:bomb-x0y0 bomb
              :fire {:timestamp 1000000010000}
              :player-1 player}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {:bomb-x0y0 {:x 0, :y 0}}}
          arena (a/eval arena (d/make-timestamp))
          {{v :v, :as grid} :grid, players :players} arena
          cell (first v)]
      (:bomb-x0y0 cell) => nil?
      (:fire cell) => nil?
      (:player-1 cell) => nil?
      (:player-1 players) => nil?))

  (fact "a game is over when less than 2 players are alive"
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ (d/make-timestamp) config/bomb-timeout-ms)
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          hbl (d/make-cell-hard-block)
          v [pl1 pl2 nil
             nil hbl nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 0, :y 0}, :player-2 {:x 1, :y 0}}
                 :bombs {}}]
      (fact "last man standing wins"
        (let [arena (-> arena
                        (a/plant-bomb :player-1 ts-1)
                        (a/move :player-1 :down)
                        (a/move :player-1 :down)  ; pass block
                        (a/move :player-1 :right)
                        (a/eval ts-2))
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 1
          (:gameover arena) => {:winner :player-1, :timestamp ts-2}))

      (fact "no winner on empty arena"
        (let [arena (-> arena
                        (a/plant-bomb :player-1 ts-1)
                        (a/move :player-1 :down)
                        (a/eval ts-2))
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 2
          (:gameover arena) => {:timestamp ts-2}))

      (fact "with 2 players, the game is ongoing"
        (let [arena (a/eval arena ts-2)
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 0
          (:gameover arena) => nil?))
  ))
)
