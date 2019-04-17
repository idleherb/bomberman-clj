(ns bomberman-clj.game-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.game :as g]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))

(defn- player-with-coords
  [player coords]
  (assoc player :coords coords))

(facts "about games"
  (fact "a game is initialized from given width, height, without any players and grid"
    ; (println "001")
    (let [timestamp (d/make-timestamp)
          w 17
          h 15
          game (g/init 2 w h)
          {:keys [grid in-progress? num-players players width height]} game]
      grid => nil?
      in-progress? => false
      num-players => 2
      (count players) => 0
      width => w
      height => h))

  (fact "at max `num-players` can join a game"
    ; (println "002")
    (let [timestamp (d/make-timestamp)
          game (-> (d/make-empty-game)
                   (g/join (d/make-player-1) timestamp)
                   (g/join (d/make-player-2) timestamp)
                   (g/join (d/make-player-3) timestamp))
          players (:players game)]
      (count players) => 2))

  (fact "a new round can only be started after all players have joined"
    ; (println "003")
    (let [timestamp (d/make-timestamp)
          game (-> (d/make-empty-game)
                   (g/join (d/make-player-1) timestamp)
                   (g/next-round timestamp))
          {:keys [grid in-progress?]} game]
      grid => nil?
      in-progress? => false
      (let [game (-> game
                     (g/join (d/make-player-2) timestamp)
                     (g/next-round timestamp))
                     {:keys [grid in-progress?]} game]
        grid => some?
        in-progress? => true)))

  (fact "a player can move to NESW empty cells if a game is in progress"
    ; (println "004")
    (let [pl1 (d/make-cell-p1)
          v [nil nil nil
             nil pl1 nil
             nil nil nil]
          players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})}
          game (-> {:in-progress? false
                    :width 3
                    :height 3
                    :num-players 1
                    :grid {:width 3, :height 3, :v v}
                    :players players}
                    (g/move :player-1 :down (d/make-timestamp)))
          {players :players, {v :v} :grid} game
          coords (:coords (:player-1 players))]
      coords => {:x 1, :y 1}
      (nth v 4) => {:player-id :player-1}
      (let [game (-> (assoc game :in-progress? true)
                     (g/move :player-1 :down (d/make-timestamp))  ; hit block
                     (g/move :player-1 :left (d/make-timestamp))
                     (g/move :player-1 :left (d/make-timestamp))  ; hit block
                     (g/move :player-1 :left (d/make-timestamp))  ; hit block
                     (g/move :player-1 :up (d/make-timestamp))
                     (g/move :player-1 :up (d/make-timestamp))
                     (g/move :player-1 :up (d/make-timestamp))  ; hit block
                     (g/move :player-1 :up (d/make-timestamp)))  ; hit block
            {players :players, {v :v} :grid} game
            coords (:coords (:player-1 players))]
            coords => {:x 0, :y 0}
        (first v) => {:player-id :player-1}
        (nth v 4) => nil?)))

  (fact "a player can't move through solid blocks"
    ; (println "005")
    (let [plr (d/make-cell-p1)
          hbl {:block :hard}
          v [nil nil nil
             hbl plr nil
             nil hbl nil]
          players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})}
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 1
                :grid {:width 3 :height 3 :v v}
                :players players}
          game (g/move game :player-1 :down (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :down (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :left (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :left (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :left (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :up (d/make-timestamp))
          game (g/move game :player-1 :up (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :up (d/make-timestamp))  ; hit block
          game (g/move game :player-1 :up (d/make-timestamp))  ; hit block
          {players :players, {v :v} :grid} game
          coords (:coords (:player-1 players))]
      coords => {:x 1, :y 0}
      (nth v 4) => nil?))

  (fact "player-1 places a bomb at their current position"
    ; (println "006")
    (let [timestamp (d/make-timestamp)
          v [(d/make-cell-p1)]
          game {:in-progress? true
                :width 1
                :height 1
                :num-players 1
                :grid {:width 1, :height 1, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})}}
          game (g/plant-bomb game :player-1 timestamp)
          {{v :v} :grid} game
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "in one cell, only 1 bomb can be planted at any moment in time"
    ; (println "007")
    (let [ts-1st 1000000000000
          ts-2nd 2000000000000
          v [(d/make-cell-p1)]
          game {:in-progress? true
                :width 1
                :height 1
                :num-players 1
                :grid {:width 1, :height 1, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})}}
          game (g/plant-bomb game :player-1 ts-1st)
          game (g/plant-bomb game :player-1 ts-2nd)
          {{v :v} :grid} game
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp ts-1st}))

  (fact "a planted bomb is still there after the player moves away"
    ; (println "008")
    (let [timestamp (d/make-timestamp)
          v [(d/make-cell-p1) nil]
          game (-> {:in-progress? true
                    :width 1
                    :height 2
                    :num-players 1
                    :grid {:width 1, :height 2, :v v}
                    :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})}}
                   (g/plant-bomb :player-1 timestamp)
                   (g/move :player-1 :down (d/make-timestamp)))
          {{v :v} :grid} game
          cell (first v)]
      (:bomb cell) => {:player-id :player-1, :timestamp timestamp}))

  (fact "a player can't plant more bombs than they have"
    ; (println "009")
    (let [timestamp (d/make-timestamp)
          plr (d/make-cell-p1)
          v [plr
             nil
             nil]
          game (-> {:in-progress? true
                    :width 1
                    :height 3
                    :num-players 1
                    :grid {:width 1, :height 3, :v v}
                    :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})}}
                   (g/plant-bomb :player-1 timestamp)
                   (g/move :player-1 :down timestamp)
                   (g/plant-bomb :player-1 timestamp)
                   (g/move :player-1 :down timestamp)
                   (g/plant-bomb :player-1 timestamp))
          {{v :v} :grid, players :players} game
          player (:player-1 players)
          bomb {:player-id :player-1, :timestamp timestamp}]
      (:bomb (first v)) => bomb
      (:bomb (second v)) => nil?
      (:bomb (nth v 2)) => nil?
      (:bomb-count player) => 0))

  (fact "an evaluated game without any bombs has no changes"
    ; (println "010")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [nil nil pl2
             nil pl1 nil
             nil nil nil]
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 2
                :grid {:width 3, :height 3, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})
                          :player-2 (player-with-coords (d/make-player-2) {:x 0, :y 2})}}]
      (g/eval game (d/make-timestamp)) => game))

  (fact "an evaluated game with an expired bomb contains fire"
    ; (println "011")
    (let [timestamp (d/make-timestamp)
          bom {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          hbl (d/make-cell-hard-block)
          sbl (d/make-cell-soft-block)
          plr (d/make-cell-p1)
          v [bom hbl nil
             nil plr nil
             sbl nil nil]
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 1
                :grid {:width 3, :height 3, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})}}
          {{v :v} :grid, players :players, :as evaluated-game} (g/eval game timestamp)
          player (:player-1 players)]
      evaluated-game =not=> game
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
      (:hit player) => nil?
      (:hit (:block (nth v 6))) => {:timestamp timestamp}))

  (fact "an evaluated game with an expired bomb hits the nearby player"
    ; (println "012")
    (let [timestamp (d/make-timestamp)
          bmb {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          plr (d/make-cell-p1)
          v [bmb nil nil
             nil nil bmb
             plr nil nil]
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 1
                :grid {:width 3, :height 3, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 2})}}
          {{v :v} :grid, players :players, :as evaluated-game} (g/eval game timestamp)
          player (:player-1 players)]
      evaluated-game =not=> game
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
      (:hit player) => {:timestamp timestamp}))

  (fact "bomb detonations propagate to nearby bombs, leaving others unchanged"
    ; (println "013")
    (let [bm1 {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          bm2 {:bomb {:player-id :player-1, :timestamp 9999999999999}}
          plr (d/make-cell-p1)
          v [bm1 nil bm2
             nil plr nil
             nil bm2 nil]
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 1
                :grid {:width 3, :height 3, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})}}
          {{v :v, :as grid} :grid, players :players, :as evaluated-game} (g/eval game 2222222222222)
          player (:player-1 players)]
      evaluated-game =not=> game
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
      (:hit player) => nil?))

  (fact "a player walking into a cell with fire gets hit"
    ; (println "014")
    (let [timestamp 1000000003022
          bom {:bomb {:player-id :player-1, :timestamp 1000000000000}}
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [bom nil nil
             nil pl1 pl2]
          game {:in-progress? true
                :width 3
                :height 2
                :num-players 2
                :grid {:width 3, :height 2, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})
                          :player-2 (player-with-coords (d/make-player-2) {:x 2, :y 1})}}
          game (g/eval game 1000000003000)
          game (g/move game :player-1 :up 1000000003022)
          {{v :v} :grid, players :players, :as game} game
          player-1 (:player-1 players)]
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
      (:hit player-1) => {:timestamp timestamp}))
     
  (fact "at game evaluation, expired detonated bombs, fire and hit players get removed"
    ; (println "015")
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
              :player-id :player-1}]
          game {:in-progress? true
                :width 1
                :height 1
                :num-players 1
                :grid {:width 1, :height 1, :v v}
                :players {:player-1 (player-with-coords player {:x 0, :y 0})}}
          game (g/eval game (d/make-timestamp))
          {{v :v, :as grid} :grid, players :players} game
          cell (first v)]
      (:bomb cell) => nil?
      (:fire cell) => nil?
      (:player-id cell) => nil?
      (:coords (:player-1 players)) => nil?))

  (fact "a game is over when less than 2 players are alive"
    ; (println "016")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ (d/make-timestamp) config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms)
          pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          hbl (d/make-cell-hard-block)
          v [pl1 pl2 nil
             nil hbl nil
             nil nil nil]
          game {:in-progress? true
                :width 3
                :height 3
                :num-players 2
                :grid {:width 3, :height 3, :v v}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})
                          :player-2 (player-with-coords (d/make-player-2) {:x 1, :y 0})}}]
    (fact "last man standing wins"
      ; (println "017")
      (let [game (-> game
                      (g/plant-bomb :player-1 ts-1)
                      (g/move :player-1 :down ts-1)
                      (g/move :player-1 :down ts-1)  ; pass block
                      (g/move :player-1 :right ts-1)
                      (g/eval ts-2)
                      (g/eval ts-3))
            players (:players game)]
        (count players) => 2
        (count (filter (comp nil? :coords second) players)) => 1
        (:gameover game) => {:winner :player-1, :timestamp ts-3}))

    (fact "no winner on empty game"
      ; (println "018")
      (let [game (-> game
                      (g/plant-bomb :player-1 ts-1)
                      (g/move :player-1 :down (d/make-timestamp))
                      (g/eval ts-2)
                      (g/eval ts-3))
            players (:players game)]
        (count players) => 2
        (count (filter (comp nil? :coords second) players)) => 2
        (:gameover game) => {:timestamp ts-3}))

    (fact "with 2 players, the game is ongoing"
      ; (println "019")
      (let [game (g/eval game ts-2)
            players (:players game)]
        (count players) => 2
        (count (filter (comp nil? :coords second) players)) => 0
        (:gameover game) => nil?))))

  (fact "collecting a bomb item increases a player's bomb count"
    ; (println "020")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          itm (d/make-cell-item-bomb)
          game {:in-progress? true
                :width 2
                :height 2
                :num-players 2
                :grid {:width 2, :height 2
                       :v [pl1 pl2
                           itm nil]}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})
                          :player-2 (player-with-coords (d/make-player-2) {:x 1, :y 0})}}
            player-1 (:player-1 (:players game))]
          game (g/eval game (d/make-timestamp))
      (:bomb-count player-1) => 1
      (let [game (g/move game :player-1 :down (d/make-timestamp))
            game (g/eval game (d/make-timestamp))
            {{v :v, :as grid} :grid, players :players} game
            player-1 (:player-1 players)]
        player-1 => some?
        (:bomb-count player-1) => 2)))

  (fact "fire hits items, hit items disappear after a while"
    ; (println "021")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          itm (d/make-cell-item-bomb)
          ts-1 (d/make-timestamp)
          ts-2 (+ config/bomb-timeout-ms (d/make-timestamp))
          ts-3 (+ config/expiration-ms ts-2)
          bmb {:bomb {:player-id :player-1, :timestamp ts-1}}
          game {:in-progress? true
                :width 3
                :height 2
                :num-players 2
                :grid {:width 3, :height 2
                       :v [pl1 pl2 nil
                           itm nil bmb]}
                :players {:player-1 (player-with-coords (d/make-player-1) {:x 0, :y 0})
                          :player-2 (player-with-coords (d/make-player-2) {:x 1, :y 0})}}
          game (g/eval game ts-2)
          {{v :v} :grid} game]
      (:hit (:item (nth v 3))) => {:timestamp ts-2}
      (let [game (g/eval game ts-3)
            {{v :v} :grid} game]
        (:item (nth v 3)) => nil?)))

  (fact "players who leave the game, die"
    ; (println "022")
    (let [pl1 (d/make-cell-p1)
          pl2 (d/make-cell-p2)
          v [nil nil pl2
             nil pl1 nil
             nil nil nil]
          timestamp (d/make-timestamp)
          game (-> {:in-progress? true
                    :width 3
                    :height 3
                    :num-players 2
                    :grid {:width 3, :height 3, :v v}
                    :players {:player-1 (player-with-coords (d/make-player-1) {:x 1, :y 1})
                              :player-2 (player-with-coords (d/make-player-2) {:x 2, :y 1})}}
                   (g/leave :player-1 timestamp))
          {{v :v} :grid, players :players} game
          player-1 (:player-1 players)
          player-2 (:player-2 players)]
      (:hit player-1) => {:timestamp timestamp}
      (:hit player-2) => nil?))
)
