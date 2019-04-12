(ns bomberman-clj.arena-test
  (:require [midje.sweet :refer [fact facts future-fact => =not=> tabular]]
            [bomberman-clj.arena :as a]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))
(facts "about arenas"
  (fact "initializing an arena of size 17x15 with 2 players and 0 bombs"
    ; (println "001")
    (let [width 17
          height 15
          players {:player-1 {:glyph \P, :name "White Bomberman"}
                   :player-2 {:glyph \Q, :name "Pretty Bomber"}}
          arena (a/init width height players)
          {{v :v} :grid, players :players} arena]
      (count v) => (* width height)
      (count (filter (complement nil?) v)) => #(>= % (+ 2 (* 8 7)))
      (count players) => 2))

  (fact "a player can move to NESW empty cells"
    ; (println "002")
    (let [pl1 (d/make-cell-p1)
          v [nil nil nil
             nil pl1 nil
             nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena (-> {:grid {:width 3, :height 3, :v v}, :players players}
                    (a/move ,,, :player-1 :down (d/make-timestamp))
                    (a/move ,,, :player-1 :down (d/make-timestamp))  ; hit block
                    (a/move ,,, :player-1 :left (d/make-timestamp))
                    (a/move ,,, :player-1 :left (d/make-timestamp))  ; hit block
                    (a/move ,,, :player-1 :left (d/make-timestamp))  ; hit block
                    (a/move ,,, :player-1 :up (d/make-timestamp))
                    (a/move ,,, :player-1 :up (d/make-timestamp))
                    (a/move ,,, :player-1 :up (d/make-timestamp))  ; hit block
                    (a/move ,,, :player-1 :up (d/make-timestamp)))  ; hit block
          {players :players, {v :v} :grid} arena
          {x :x, y :y} (:player-1 players)]
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
          {players :players, {v :v} :grid} arena
          {x :x, y :y} (:player-1 players)]
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
           {{v :v} :grid} arena
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "in one cell, only one bomb can be planted at any moment in time"
    ; (println "005")
    (let [ts-1st 1000000000000
          ts-2nd 2000000000000
          v [(d/make-cell-p1)]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 ts-1st)
          arena (a/plant-bomb arena :player-1 ts-2nd)
          {{v :v} :grid} arena
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp ts-1st}))

  (fact "a planted bomb is still there after the player moves away"
    ; (println "006")
    (let [timestamp (d/make-timestamp)
          v [(d/make-cell-p1) nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/plant-bomb arena :player-1 timestamp)
          arena (a/move arena :player-1 :down (d/make-timestamp))
          {{v :v} :grid} arena
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "a player can't plant more bombs than they have"
    ; (println "007")
    (let [timestamp (d/make-timestamp)
          plr (d/make-cell-p1)
          v [plr
             nil
             nil]
          arena (-> {:grid {:width 1, :height 3, :v v}
                     :players {:player-1 {:x 0, :y 0}}}
                    (a/plant-bomb ,,, :player-1 timestamp)
                    (a/move ,,, :player-1 :down timestamp)
                    (a/plant-bomb ,,, :player-1 timestamp)
                    (a/move ,,, :player-1 :down timestamp)
                    (a/plant-bomb ,,, :player-1 timestamp))
          {{v :v} :grid} arena
          player (:player (nth v 2))
          bomb {:player-id :player-1, :timestamp timestamp}]
      (:bomb (first v)) => bomb
      (:bomb (second v)) => nil?
      (:bomb (nth v 2)) => nil?
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
          bom {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          plr (d/make-cell-p1)
          v [bom hbl nil
             nil plr nil
             sbl nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}}
          {{v :v} :grid, :as evaluated-arena} (a/eval arena timestamp)]
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
      (let [bomb (:bomb (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player (nth v 4))) => nil?
      (:hit (:block (nth v 6))) => {:timestamp timestamp}))

  (fact "an evaluated arena with an expired bomb hits the nearby player"
    ; (println "010")
    (let [timestamp (d/make-timestamp)
          bmb {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          plr (d/make-cell-p1)
          v [bmb nil nil
             nil nil bmb
             plr nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 0, :y 2}}}
          {{v :v} :grid, :as evaluated-arena} (a/eval arena timestamp)]
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
      (let [bomb (:bomb (nth v 0))]
        (:detonated bomb) => {:timestamp timestamp})
      (let [bomb (:bomb (nth v 5))]
        (:detonated bomb) => {:timestamp timestamp})
      (:hit (:player (nth v 6))) => {:timestamp timestamp}))

  (fact "bomb detonations propagate to nearby bombs, leaving others unchanged"
    ; (println "011")
    (let [bm1 {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {:bomb {:player-id :player-1, :timestamp 9999999999999}}
          plr (d/make-cell-p1)
          v [bm1 nil bm2
             nil plr nil
             nil bm2 nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:x 1, :y 1}}}
          {{v :v} :grid, :as evaluated-arena} (a/eval arena 2222222222222)]
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
      (let [bomb (:bomb (nth v 0))]
        (:detonated bomb) => {:timestamp 2222222222222})
      (let [bomb (:bomb (nth v 2))]
        (:detonated bomb) => {:timestamp 2222222222222})
      (let [bomb (:bomb (nth v 7))]
        (:detonated bomb) => nil?)
      (:hit (:player (nth v 4))) => nil?))

  (fact "a player walking into a cell with fire gets hit"
    ; (println "012")
    (let [timestamp 1000000003022
          bom {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [bom nil nil
             nil pl1 pl2]
          arena {:grid {:width 3, :height 2, :v v}
                 :players {:player-1 {:x 1, :y 1}, :player-2 {:x 2, :y 1}}}
          arena (a/eval arena 1000000003000)
          arena (a/move arena :player-1 :up 1000000003022)
          {{v :v} :grid, :as arena} arena]
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
      (let [bomb (:bomb (nth v 0))]
        (:detonated bomb) => {:timestamp 1000000003000})
      (:hit (:player (nth v 1))) => {:timestamp timestamp}))
     
  (fact "at arena evaluation, expired detonated bombs, fire and hit players get removed"
    ; (println "013")
    (let [bomb {:detonated {:timestamp 1000000010000}
                :player-id :player-1
                :timestamp 1000000000000}
          player {:player-id :player-1
                  :bomb-count 0
                  :bomb-radius 3
                  :glyph \P
                  :hit {:timestamp 1000000010000}
                  :name "foo"}
          v [{:bomb bomb
              :fire {:timestamp 1000000010000}
              :player player}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 {:x 0, :y 0}}}
          arena (a/eval arena (d/make-timestamp))
          {{v :v, :as grid} :grid, players :players} arena
          cell (first v)]
      (:bomb cell) => nil?
      (:fire cell) => nil?
      (:player cell) => nil?
      (:player-1 players) => nil?))

  (fact "a game is over when less than 2 players are alive"
    ; (println "014")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ (d/make-timestamp) config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms)
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
                      (a/move :player-1 :down ts-1)
                      (a/move :player-1 :down ts-1)  ; pass block
                      (a/move :player-1 :right ts-1)
                      (a/eval ts-2)
                      (a/eval ts-3))
            players (:players arena)]
        (count players) => 2
        (count (filter (comp nil? second) players)) => 1
        (:gameover arena) => {:winner :player-1, :timestamp ts-3}))

    (fact "no winner on empty arena"
      ; (println "016")
      (let [arena (-> arena
                      (a/plant-bomb :player-1 ts-1)
                      (a/move :player-1 :down (d/make-timestamp))
                      (a/eval ts-2)
                      (a/eval ts-3))
            players (:players arena)]
        (count players) => 2
        (count (filter (comp nil? second) players)) => 2
        (:gameover arena) => {:timestamp ts-3}))

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
      (:bomb-count (:player pl1)) => 1
      (let [arena (a/move arena :player-1 :down (d/make-timestamp))
            arena (a/eval arena (d/make-timestamp))
            {{v :v, :as grid} :grid} arena
            player-1 (:player (nth v 2))]
        player-1 => some?
        (:bomb-count player-1) => 2)))

  (future-fact "fire hits items, hit items disappear after a while"
    ; (println "019")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          itm (d/make-cell-item-bomb)
          bmb {:bomb {:player-id :player-1, :timestamp 1552767537306}}
          arena {:players {:player-1 {:x 0, :y 0}
                           :player-2 {:x 1, :y 0}}
                 :grid {:width 3, :height 2
                        :v [pl1 pl2 nil
                            itm nil bmb]}}
          timestamp (+ config/bomb-timeout-ms (d/make-timestamp))
          arena (a/eval arena timestamp)
          {{v :v} :grid} arena]
      (:hit (:item (nth v 3))) => {:timestamp timestamp}
      (let [arena (a/eval arena (+ config/expiration-ms timestamp))]
        (:item (nth v 3)) => nil?)))
)
