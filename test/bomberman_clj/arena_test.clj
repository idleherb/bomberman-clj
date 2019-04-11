(ns bomberman-clj.arena-test
  (:require [midje.sweet :refer [fact facts future-fact => =not=> tabular]]
            [bomberman-clj.arena :as a]
            [bomberman-clj.cells :as cells]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))
(facts "about arenas"
  (fact "initializing an arena of size 17x15 with 2 players and 0 bombs"
    ; (println "001")
    (let [width 17
          height 15
          players {:player-1 {:glyph \P}, :player-2 {:glyph \Q}}
          arena (a/init width height players)
          {{v :v, :as grid} :grid, players :players} arena]
      (count v) => (* width height)
      (count (filter (complement nil?) v)) => #(>= % (+ 2 (* 8 7)))
      (count players) => 2))

  (fact "a player can move to NESW empty cells"
    ; (println "002")
    (let [plr (d/make-cell-p1)
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:grid {:width 3 :height 3 :v v} :players players}
          arena (a/move arena :player-1 :down (d/make-timestamp))
          arena (a/move arena :player-1 :down (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :left (d/make-timestamp))
          arena (a/move arena :player-1 :left (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :left (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :up (d/make-timestamp))
          arena (a/move arena :player-1 :up (d/make-timestamp))
          arena (a/move arena :player-1 :up (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :up (d/make-timestamp))  ; hit block
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 0
      y => 0
      (nth v 4) => nil?))

  (fact "a player can't move through solid blocks"
    ; (println "003")
    (let [plr (d/make-cell-p1)
          hbl {:block :hard}
          v [nil nil nil
             hbl plr nil
             nil hbl nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:grid {:width 3 :height 3 :v v} :players players}
          arena (a/move arena :player-1 :down (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :down (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :left (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :left (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :left (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :up (d/make-timestamp))
          arena (a/move arena :player-1 :up (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :up (d/make-timestamp))  ; hit block
          arena (a/move arena :player-1 :up (d/make-timestamp))  ; hit block
          {players :players {v :v} :grid} arena
          {x :x, y :y, :as coords} (:player-1 players)]
      x => 1
      y => 0
      (nth v 4) => nil?))

  (fact "player-1 places a bomb at their current position"
    ; (println "004")
    (let [timestamp (d/make-timestamp)
          v [(d/make-cell-p1)]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, :as arena} arena
          cell (first v)]
      (:bomb-x0y0 cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "in one cell, only one bomb can be planted at any moment in time"
    ; (println "005")
    (let [ts-1st 1000000000000
          ts-2nd 2000000000000
          v [(d/make-cell-p1)]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 ts-1st)
          arena (a/plant-bomb arena :player-1 ts-2nd)
          {{v :v, :as grid} :grid, :as arena} arena
          cell (first v)]
      (:bomb-x0y0 cell) => {:player-id :player-1, :timestamp ts-1st}))

  (fact "a planted bomb is still there after the player moves away"
    ; (println "006")
    (let [timestamp (d/make-timestamp)
          v [(d/make-cell-p1) nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, :as arena} (a/move arena :player-1 :down (d/make-timestamp))
          cell (first v)]
          (:bomb-x0y0 cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "a player can't plant more bombs than they have"
    ; (println "007")
    (let [timestamp (d/make-timestamp)
          v [{:player-1 {:glyph \P, :bomb-count 2}} nil nil nil]
          arena {:grid {:width 1, :height 4, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down (d/make-timestamp))
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down (d/make-timestamp))
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down (d/make-timestamp))
          arena (a/plant-bomb arena :player-1 timestamp)
          {{v :v, :as grid} :grid, :as arena} arena
          cell (nth v 3)
          player (:player-1 cell)]
      (cells/cell-bomb (first v)) => (complement nil?)
      (cells/cell-bomb (second v)) => (complement nil?)
      (cells/cell-bomb (nth v 2)) => nil?
      (cells/cell-bomb (nth v 3)) => nil?
      (:bomb-count player) => 0))

  (fact "an evaluated arena without any bombs has no changes"
    ; (println "008")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [nil nil pl2
             nil pl1 nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}
                           :player-2 {:x 0, :y 2}}}]
      (a/eval arena (d/make-timestamp)) => arena))

  (fact "an evaluated arena with an expired bomb contains fire"
    ; (println "009")
    (let [timestamp (d/make-timestamp)
          hbl (d/make-cell-hard-block)
          sbl (d/make-cell-soft-block)
          bomb-id :bomb-x0y0
          bom {bomb-id {:player-id :player-1, :timestamp 1000000000000}}
          plr (d/make-cell-p1)
          v [bom hbl nil
             nil plr nil
             sbl nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}}
          {{v :v, :as grid} :grid
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
      (let [bomb (bomb-id (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player-1 (nth v 4))) => nil?
      (:hit (:block (nth v 6))) => {:timestamp timestamp}))

  (fact "an evaluated arena with an expired bomb hits the nearby player"
    ; (println "010")
    (let [timestamp (d/make-timestamp)
          bomb-id1 :bomb-x0y0
          bomb-id2 :bomb-x2y1
          bm1 {bomb-id1 {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {bomb-id2 {:player-id :player-1, :timestamp 1000000000000}}
          plr (d/make-cell-p1)
          v [bm1 nil nil
             nil nil bm2
             plr nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 0, :y 2}}}
          {{v :v, :as grid} :grid
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
      (let [bomb (bomb-id1 (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (let [bomb (bomb-id2 (nth v 5))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player-1 (nth v 6))) => {:timestamp timestamp}))

  (fact "bomb detonations propagate to nearby bombs, leaving others unchanged"
    ; (println "011")
    (let [bm1 {:bomb-x0y0 {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {:bomb-x2y0 {:player-id :player-1, :timestamp 9999999999999}}
          bm3 {:bomb-x1y2 {:player-id :player-1, :timestamp 9999999999999}}
          plr (d/make-cell-p1)
          v [bm1 nil bm2
             nil plr nil
             nil bm3 nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}}
          {{v :v, :as grid} :grid
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
      (:hit (:player-1 (nth v 4))) => nil?))

  (fact "a player walking into a cell with fire gets hit"
    ; (println "012")
    (let [timestamp 1000000003022
          bom {:bomb-x0y0 {:player-id :player-1, :timestamp 1000000000000}}
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [bom nil nil
             nil pl1 pl2]
          arena {:grid {:width 3, :height 2, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 2, :y 1}}}
          arena (a/eval arena 1000000003000)
          arena (a/move arena :player-1 :up 1000000003022)
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
    ; (println "013")
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
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/eval arena (d/make-timestamp))
          {{v :v, :as grid} :grid, players :players} arena
          cell (first v)]
      (:bomb-x0y0 cell) => nil?
      (:fire cell) => nil?
      (:player-1 cell) => nil?
      (:player-1 players) => nil?))

  (fact "a game is over when less than 2 players are alive"
    ; (println "014")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ (d/make-timestamp) config/bomb-timeout-ms)
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          hbl (d/make-cell-hard-block)
          v [pl1 pl2 nil
             nil hbl nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 0, :y 0}, :player-2 {:x 1, :y 0}}}]
    (fact "last man standing wins"
      ; (println "015")
      (let [arena (-> arena
                      (a/plant-bomb :player-1 ts-1)
                      (a/move :player-1 :down (d/make-timestamp))
                      (a/move :player-1 :down (d/make-timestamp))  ; pass block
                      (a/move :player-1 :right (d/make-timestamp))
                      (a/eval ts-2))
            players (:players arena)]
        (count players) => 2
        (count (filter (comp nil? second) players)) => 1
        (:gameover arena) => {:winner :player-1, :timestamp ts-2}))

    (fact "no winner on empty arena"
      ; (println "016")
      (let [arena (-> arena
                      (a/plant-bomb :player-1 ts-1)
                      (a/move :player-1 :down (d/make-timestamp))
                      (a/eval ts-2))
            players (:players arena)]
        (count players) => 2
        (count (filter (comp nil? second) players)) => 2
        (:gameover arena) => {:timestamp ts-2}))

    (fact "with 2 players, the game is ongoing"
      ; (println "017")
      (let [arena (a/eval arena ts-2)
            players (:players arena)]
        (count players) => 2
        (count (filter (comp nil? second) players)) => 0
        (:gameover arena) => nil?))))

  (fact "collecting a bomb item increases a player's bomb count"
    ; (println "018")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          itm (d/make-cell-item-bomb)
          arena {:players {:player-1 {:x 0, :y 0}
                           :player-2 {:x 1, :y 0}}
                 :grid {:width 2, :height 2
                        :v [pl1 pl2
                            itm nil]}}]
          arena (a/eval arena (d/make-timestamp))
      (:bomb-count (:player-1 pl1)) => 1
      (let [arena (a/move arena :player-1 :down (d/make-timestamp))
            arena (a/eval arena (d/make-timestamp))
            {{v :v, :as grid} :grid} arena
            player-1 (:player-1 (nth v 2))]
        player-1 => some?
        (:bomb-count player-1) => 2)))

  (future-fact "fire hits items, hit items disappear after a while"
    (println "019")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          itm (d/make-cell-item-bomb)
          bmb {:bomb-x2y1 {:player-id :player-1, :timestamp 1552767537306}}
          arena {:players {:player-1 {:x 0, :y 0}
                           :player-2 {:x 1, :y 0}}
                 :grid {:width 3, :height 2
                        :v [pl1 pl2 nil
                            itm nil bmb]}}
          timestamp (+ config/bomb-timeout-ms (d/make-timestamp))
          arena (a/eval arena timestamp)
          {{v :v} :grid} arena]
      (println "!!!" arena)
      (:hit (:item (nth v 3))) => {:timestamp timestamp}
      (let [arena (a/eval arena (+ config/expiration-ms timestamp))]
        (:item (nth v 3)) => nil?)))
)
