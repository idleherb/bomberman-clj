(ns bomberman-clj.arena-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.arena :refer [detonate-bomb
                                         eval-arena
                                         init-arena
                                         move
                                         plant-bomb]]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :refer [make-timestamp]]))

(facts "about arenas"
  (fact "initializing an arena of size 17x15 with 2 players and 0 bombs"
    (let [width 17
          height 15
          players {:player-1 {:glyph \P}, :player-2 {:glyph \Q}}
          arena (init-arena width height players)
          {{v :v, :as grid} :grid, players :players, bombs :bombs} arena]
      (count v) => (* width height)
      (count (filter (complement nil?) v)) => 2
      (count players) => 2
      (count bombs) => 0))

  (fact "a player can move to NESW empty cells"
    (let [plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          arena (move arena :player-1 :down)
          arena (move arena :player-1 :down)  ; hit wall
          arena (move arena :player-1 :left)
          arena (move arena :player-1 :left)  ; hit wall
          arena (move arena :player-1 :left)  ; hit wall
          arena (move arena :player-1 :up)
          arena (move arena :player-1 :up)
          arena (move arena :player-1 :up)  ; hit wall
          arena (move arena :player-1 :up)  ; hit wall
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 0
      y => 0
      (nth v 4) => nil?))

  (fact "a player can't move through solid walls"
    (let [plr {:player-1 {:glyph \P, :bomb-count 3}}
          wal {:wall :solid, :glyph \█}
          v [nil nil nil
             wal plr nil
             nil wal nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          arena (move arena :player-1 :down)  ; hit wall
          arena (move arena :player-1 :down)  ; hit wall
          arena (move arena :player-1 :left)  ; hit wall
          arena (move arena :player-1 :left)  ; hit wall
          arena (move arena :player-1 :left)  ; hit wall
          arena (move arena :player-1 :up)
          arena (move arena :player-1 :up)  ; hit wall
          arena (move arena :player-1 :up)  ; hit wall
          arena (move arena :player-1 :up)  ; hit wall
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 1
      y => 0
      (nth v 4) => nil?))

  (fact "player-1 places a bomb at their current position"
    (let [timestamp (make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 3}}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (plant-bomb arena :player-1 timestamp)
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
          arena (plant-bomb arena :player-1 ts-1st)
          arena (plant-bomb arena :player-1 ts-2nd)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (count bombs) => 1
      (let [bomb (:bomb-x0y0 bombs)]
        bomb => map?
        (count bomb) => 2)
      (:timestamp (:bomb-x0y0 cell)) => ts-1st))

  (fact "a planted bomb is still there after the player moves away"
    (let [timestamp (make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 3}} nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} (move arena :player-1 :down)
          cell (first v)]
      (count bombs) => 1
      (:timestamp (:bomb-x0y0 cell)) => timestamp))

  (fact "a player can't plant more bombs than they have"
    (let [timestamp (make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 2}} nil nil nil]
          arena {:grid {:width 1, :height 4, :v v}
                 :players {:player-1 {:x 0, :y 0}}
                 :bombs {}}
          arena (plant-bomb arena :player-1 timestamp)
          arena (move arena :player-1 :down)
          arena (plant-bomb arena :player-1 timestamp)
          arena (move arena :player-1 :down)
          arena (plant-bomb arena :player-1 timestamp)
          arena (move arena :player-1 :down)
          arena (plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (nth v 3)
          player (:player-1 cell)]
      (count bombs) => 2
      (cells/cell-bomb (first v)) => (complement nil?)
      (cells/cell-bomb (second v)) => (complement nil?)
      (cells/cell-bomb (nth v 2)) => nil?
      (cells/cell-bomb (nth v 3)) => nil?
      (:bomb-count player) => 0))

  (fact "a detonating bomb spreads horizontally and vertically, passing by the offset player"
    (let [timestamp (make-timestamp)
          wal {:wall :solid, :glyph \█}
          bomb-id :bomb-x1y1
          bom {bomb-id {:player-id :player-1, :timestamp 1000000000000}}
          plr {:player-1 {:glyph \P, :bomb-count 0}}
          v [nil nil nil nil nil
             nil bom nil wal nil
             nil nil plr nil nil
             nil nil nil nil nil]
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 {:x 2, :y 2}}
                 :bombs {bomb-id {:x 1, :y 1}}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena}
            (detonate-bomb arena bomb-id timestamp)
          player-1 (:player-1 (nth v 12))]
      (count bombs) => 1
      (let [bomb (bomb-id (nth v 6))]
        (:detonated bomb) => {:timestamp timestamp})
      player-1 => {:glyph \P, :bomb-count 1}
      (nth v 8) => wal
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp timestamp})
          ?cell
          (nth v 1)
          (nth v 5)
          (nth v 6)
          (nth v 7)
          (nth v 11)
          (nth v 16))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 0)
          (nth v 2)
          (nth v 3)
          (nth v 4)
          (nth v 8)
          (nth v 9)
          (nth v 10)
          (nth v 12)
          (nth v 14)
          (nth v 15)
          (nth v 17)
          (nth v 18)
          (nth v 19))))

  (fact "a detonating bomb spreads through the nearby player"
    (let [timestamp (make-timestamp)
          bomb-id :bomb-x1y1
          bom {bomb-id {:player-id :player-1, :timestamp 1000000000000}}
          plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [nil nil nil nil nil
             nil bom nil nil nil
             nil plr nil nil nil
             nil nil nil nil nil]
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 {:x 1, :y 2}}
                 :bombs {bomb-id {:x 1, :y 1}}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena}
            (detonate-bomb arena bomb-id timestamp)]
        (count bombs) => 1
        (let [bomb (bomb-id (nth v 6))]
          (:detonated bomb) => {:timestamp timestamp})
        (:player-1 (nth v 11)) => (complement nil?)
        (tabular
          (fact "cells with fire"
            (:fire ?cell) => {:timestamp timestamp})
            ?cell
            (nth v 1)
            (nth v 5)
            (nth v 7)
            (nth v 8)
            (nth v 11)
            (nth v 16))
        (tabular
          (fact "cells without fire"
            (:fire ?cell) => nil?)
            ?cell
            (nth v 0)
            (nth v 2)
            (nth v 3)
            (nth v 4)
            (nth v 9)
            (nth v 10)
            (nth v 12)
            (nth v 14)
            (nth v 15)
            (nth v 17)
            (nth v 18)
            (nth v 19))))

  (fact "an evaluated arena without any bombs has no changes"
    (let [pl1 {:player-1 {:glyph \P, :bomb-count 1}}
          pl2 {:player-2 {:glyph \Q, :bomb-count 1}}
          v [nil nil pl2
             nil pl1 nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 0, :y 2}}
                 :bombs {}}]
          (eval-arena arena (make-timestamp)) => arena))

  (fact "an evaluated arena with an expired bomb contains fire"
    (let [timestamp (make-timestamp)
          bomb-id :bomb-x0y0
          bom {bomb-id {:player-id :player-1, :timestamp 1000000000000}}
          plr {:player-1 {:glyph \P, :bomb-count 3}}
          v [bom nil nil
             nil plr nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}
                 :bombs {bomb-id {:x 0, :y 0}}}
          {{v :v, :as grid} :grid
            bombs :bombs
            :as evaluated-arena} (eval-arena arena timestamp)]
      evaluated-arena =not=> arena
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp timestamp})
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2)
          (nth v 3)
          (nth v 6))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 4)
          (nth v 5)
          (nth v 7)
          (nth v 8))
      (count bombs) => 1
      (let [bomb (bomb-id (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player-1 (nth v 4))) => nil?))

  (fact "an evaluated arena with an expired bomb hits the nearby player"
    (let [timestamp (make-timestamp)
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
            :as evaluated-arena} (eval-arena arena timestamp)]
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
            :as evaluated-arena} (eval-arena arena 2222222222222)]
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

  (fact "a player walking into a cell with fire gets hit"
    (let [timestamp 1000000003022
          bom {:bomb-x0y0 {:player-id :player-1, :timestamp 1000000000000}}
          pl1 {:player-1 {:glyph \P, :bomb-count 3}}
          pl2 {:player-2 {:glyph \Q, :bomb-count 3}}
          v [bom nil nil
             nil pl1 pl2]
          arena {:grid {:width 3, :height 2, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 2, :y 1}}
                 :bombs {:bomb-x0y0 {:x 0, :y 0}}}
          arena (eval-arena arena 1000000003000)
          arena (move arena :player-1 :up)
          arena (eval-arena arena timestamp)
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
          arena (eval-arena arena (make-timestamp))
          {{v :v, :as grid} :grid, players :players} arena
          cell (first v)]
      (:bomb-x0y0 cell) => nil?
      (:fire cell) => nil?
      (:player-1 cell) => nil?
      (:player-1 players) => nil?))

  (fact "a game is over when less than 2 players are alive"
    (let [ts-1 (make-timestamp)
          ts-2 (+ (make-timestamp) config/bomb-timeout-ms)
          arena (init-arena 3 3 {:player-1 {:glyph \P, :bomb-count 1, :coords {:x 0 :y 0}}
                                 :player-2 {:glyph \Q, :bomb-count 1, :coords {:x 1 :y 0}}})]

      (fact "last man standing wins"
        (let [arena (-> arena
                        (plant-bomb :player-1 ts-1)
                        (move :player-1 :down)
                        (move :player-1 :right)
                        (eval-arena ts-2))
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 1
          (:gameover arena) => {:winner :player-1, :timestamp ts-2}))

      (fact "no winner on empty arena"
        (let [arena (-> arena
                        (plant-bomb :player-1 ts-1)
                        (move :player-1 :down)
                        (eval-arena ts-2))
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 2
          (:gameover arena) => {:timestamp ts-2}))

      (fact "with 2 players, the game is ongoing"
        (let [arena (eval-arena arena ts-2)
              players (:players arena)]
          (count players) => 2
          (count (filter (comp nil? second) players)) => 0
          (:gameover arena) => nil?))
  ))
)
