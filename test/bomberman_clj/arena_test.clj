(ns bomberman-clj.arena-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.arena :refer [detonate-bomb
                                         eval-arena
                                         init-arena
                                         move
                                         plant-bomb]]
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
    (let [plr {:player-1 {:glyph \P}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 [1 1]}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          arena (move arena :player-1 :south)
          arena (move arena :player-1 :south)  ; hit wall
          arena (move arena :player-1 :west)
          arena (move arena :player-1 :west)  ; hit wall
          arena (move arena :player-1 :west)  ; hit wall
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)  ; hit wall
          arena (move arena :player-1 :north)  ; hit wall
          {players :players {v :v} :grid} arena
          [x y, :as coords] (:player-1 players)]
      x => 0
      y => 0
      (nth v 4) => nil?))

  (fact "player-1 places a bomb at their current position"
    (let [timestamp (make-timestamp)
          v [{:player-1 {:glyph \P}}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 [0 0]}
                 :bombs {}}
          arena (plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (count bombs) => 1
      (let [bomb (:bomb-x0y0 bombs)]
        bomb => vector?
        (count bomb) => 2)
      (:bomb-x0y0 cell) => {:timestamp timestamp}))

  (fact "only one bomb can be planted at the same time"
    (let [ts-1st 1000000000000
          ts-2nd 2000000000000
          v [{:player-1 {:glyph \P}}]
          arena {:grid {:width 1, :height 1, :v v}
                  :players {:player-1 [0 0]}
                  :bombs {}}
          arena (plant-bomb arena :player-1 ts-1st)
          arena (plant-bomb arena :player-1 ts-2nd)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (count bombs) => 1
      (let [bomb (:bomb-x0y0 bombs)]
        bomb => vector?
        (count bomb) => 2)
      (:bomb-x0y0 cell) => {:timestamp ts-1st}))

  (fact "a planted bomb is still there after the player moves away"
    (let [timestamp (make-timestamp)
          v [{:player-1 {:glyph \P}} nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 [0 0]}
                 :bombs {}}
          arena (plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} (move arena :player-1 :south)
          cell (first v)]
      (count bombs) => 1
      (:bomb-x0y0 cell) => {:timestamp timestamp}))

  (fact "a detonating bomb spreads horizontally and vertically, passing by the offset player"
    (let [timestamp (make-timestamp)
          bomb-id :bomb-x1y1
          bom {bomb-id {:timestamp 1000000000000}}
          plr {:player-1 {:glyph \P}}
          v [nil nil nil nil nil
             nil bom nil nil nil
             nil nil plr nil nil
             nil nil nil nil nil]
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 [2 2]}
                 :bombs {bomb-id [1 1]}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena}
            (detonate-bomb arena bomb-id timestamp)]
      (count bombs) => 1
      (let [bomb (bomb-id (nth v 6))]
        (:detonated bomb) => {:timestamp timestamp})
      (:player-1 (nth v 12)) => {:glyph \P}
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp timestamp})
          ?cell
          (nth v 1)
          (nth v 5)
          (nth v 6)
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

  (fact "a detonating bomb spreads through the nearby player"
    (let [timestamp (make-timestamp)
          bomb-id :bomb-x1y1
          bom {bomb-id {:timestamp 1000000000000}}
          plr {:player-1 {:glyph \P}}
          v [nil nil nil nil nil
             nil bom nil nil nil
             nil plr nil nil nil
             nil nil nil nil nil]
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 [1 3]}
                 :bombs {bomb-id [1 1]}}
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
    (let [plr {:player-1 {:glyph \P}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 [1 1]}
                 :bombs {}}
          evaluated-arena (eval-arena arena (make-timestamp))]
      evaluated-arena => arena))

  (fact "an evaluated arena with an expired bomb contains fire"
    (let [timestamp (make-timestamp)
          bomb-id :bomb-x0y0
          bom {bomb-id {:timestamp 1000000000000}}
          plr {:player-1 {:glyph \P}}
          v [bom nil nil
             nil plr nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 [1 1]}
                 :bombs {bomb-id [0 0]}}
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
          bm1 {bomb-id1 {:timestamp 1000000000000}}
          bm2 {bomb-id2 {:timestamp 1000000000000}}
          plr {:player-1 {:glyph \P}}
          v [bm1 nil nil
             nil nil bm2
             plr nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 [0 2]}
                 :bombs {bomb-id1 [0 0]
                         bomb-id2 [2 1]}}
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
    (let [bm1 {:bomb-x0y0 {:timestamp 1000000000000}}
          bm2 {:bomb-x2y0 {:timestamp 9999999999999}}
          bm3 {:bomb-x1y2 {:timestamp 9999999999999}}
          plr {:player-1 {:glyph \P}}
          v [bm1 nil bm2
             nil plr nil
             nil bm3 nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 [1 1]}
                 :bombs {:bomb-x0y0 [0 0]
                         :bomb-x2y0 [2 0]
                         :bomb-x1y2 [1 2]}}
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
    (let [timestamp (make-timestamp)
          bom {:bomb-x0y0 {:timestamp 1000000000000}}
          plr {:player-1 {:glyph \P}}
          v [bom nil
             nil plr]
          arena {:grid {:width 2, :height 2, :v v}
                 :players {:player-1 [1 1]}
                 :bombs {:bomb-x0y0 [0 0]}}
          arena (eval-arena arena 1001111111111)
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)
          arena (eval-arena arena timestamp)
          {{v :v, :as grid} :grid, :as arena} arena]
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp 1001111111111})
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 3))
      (let [bomb (:bomb-x0y0 (nth v 0))]
        (:detonated bomb) => {:timestamp 1001111111111})
      (:hit (:player-1 (nth v 1))) => {:timestamp timestamp}))
)
