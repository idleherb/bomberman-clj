(ns bomberman-clj.game-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.game :as g]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))

(facts "about games"
  (fact "a game is initialized from given width, height, without any players and grid"
    (println "T001")
    (let [w 17
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
    (println "T002")
    (let [ts (d/make-timestamp)
          game (-> (d/make-empty-game)
                   (g/join (d/make-player :player-1) ts)
                   (g/join (d/make-player :player-2) ts)
                   (g/join (d/make-player :player-3) ts))
          {:keys [players]} game]
      (count players) => 2))

  (fact "a new round can only be started after all players have joined"
    (println "T003")
    (let [ts (d/make-timestamp)
          game (-> (d/make-empty-game)
                   (g/join (d/make-player :player-1) ts)
                   (g/next-round ts))
          {:keys [grid in-progress?]} game]
      grid => nil?
      in-progress? => false
      (let [game (-> game
                     (g/join (d/make-player :player-2) ts)
                     (g/next-round ts))
            {:keys [grid in-progress?]} game]
        grid => some?
        in-progress? => true)))

  (fact "no new round can be started if a player has left in the previous round"
    (println "T004")
    (let [ts (d/make-timestamp)
          game (-> (d/make-empty-game)
                   (g/join (d/make-player :player-1) ts)
                   (g/join (d/make-player :player-2) ts))
          players (:players game)
          player-1 (-> (:player-1 players)
                       (assoc :left {:timestamp ts}))  ; TODO: replace with g/leave
          game (-> game
                   (assoc-in [:players :player-1] player-1)
                   (g/next-round ts))
          {:keys [in-progress? players]} game
          player-1 (:player-1 players)]
      in-progress? => false
      player-1 => nil?))

  (fact "a player can move to NESW empty cells if a game is in progress"
    (println "T005")
    (let [ts (d/make-timestamp)
          game (-> (d/make-game ts)
                   (assoc :in-progress? false)
                   (g/move :player-1 :down ts))
          {players :players, {v :v} :grid} game
          coords (:coords (:player-1 players))]
      coords => {:x 0, :y 0}
      (nth v 0) => {:player-id :player-1}
      (let [game (-> (assoc game :in-progress? true)
                     (g/move :player-1 :right ts)  ; blocked by :player-2
                     (g/move :player-1 :left ts)  ; blocked by block
                     (g/move :player-1 :down ts)
                     (g/move :player-1 :down ts)
                     (g/move :player-1 :down ts)  ; blocked by world boundary
                     (g/move :player-1 :right ts)
                     (g/move :player-1 :up ts)  ; blocked by block
                     (g/move :player-1 :right ts)
                     (g/move :player-1 :up ts)
                     (g/move :player-1 :up ts))
            {players :players, {v :v} :grid} game
            coords (:coords (:player-1 players))
            cell (nth v 5)]
        coords => {:x 2, :y 1}
        cell => some?
        (:player-id cell) => :player-1
        (first v) => nil?)))

  (fact "player-1 places a bomb at their current position"
    (println "T006")
    (let [ts (d/make-timestamp)
          game (-> (d/make-game ts)
                   (g/plant-bomb :player-1 ts))
          {{v :v} :grid} game
          bomb (:bomb (first v))]
      bomb => some?
      (:player-id bomb) => :player-1
      (:timestamp bomb) => ts))

  (fact "only 1 bomb can be placed within a cell"
    (println "T007")
    (let [ts-1st (d/make-timestamp)
          ts-2nd 2000000000000
          game (-> (d/make-game ts-1st)
                   (assoc-in [:players :player-1 :bomb-count] 2)
                   (g/plant-bomb :player-1 ts-1st)
                   (g/plant-bomb :player-1 ts-2nd))
          {{v :v} :grid} game
          bomb (:bomb (first v))]
      (:timestamp bomb) => ts-1st))

  (fact "a planted bomb is still there after the player moves away"
    (println "T008")
    (let [ts (d/make-timestamp)
          game (-> (d/make-game ts)
                   (g/plant-bomb :player-1 ts)
                   (g/move :player-1 :down ts))
          {{v :v} :grid} game
          bomb (:bomb (first v))]
      bomb => some?
      (:player-id bomb) => :player-1
      (:timestamp bomb) => ts))

  (fact "a player can't plant more bombs than they have"
    (println "T009")
    (let [ts (d/make-timestamp)
          game (-> (d/make-game ts)
                   (assoc-in [:players :player-1 :bomb-count] 2)
                   (g/plant-bomb :player-1 ts)
                   (g/move :player-1 :down ts)
                   (g/plant-bomb :player-1 ts)
                   (g/move :player-1 :down ts)
                   (g/plant-bomb :player-1 ts))
          {{v :v} :grid, players :players} game
          player (:player-1 players)]
      (:bomb (first v)) => some?
      (:bomb (nth v 3)) => some?
      (:bomb (nth v 6)) => nil?
      (:bomb-count player) => 0))

  (fact "an evaluated game without any bombs has no changes"
    (println "T010")
    (let [ts (d/make-timestamp)
          game (d/make-game ts)]
      (g/eval game ts) => game))

  (fact "an evaluated game with an expired bomb contains fire"
    (println "T011")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          bomb {:player-id :player-1, :timestamp ts-1}
          bomb-idx 7
          game (-> (d/make-game ts-1)
                   (assoc-in [:grid :v bomb-idx :bomb] bomb))
          {{v :v} :grid, :as evaluated-game} (g/eval game ts-2)
          bomb (:bomb (nth v bomb-idx))]
      evaluated-game =not=> game
      (:detonated bomb) => {:timestamp ts-2}
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp ts-2, :player-id :player-1})
          ?cell
          (nth v 6)
          (nth v bomb-idx)
          (nth v 8))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 0)
          (nth v 1)
          (nth v 2)
          (nth v 3)
          (nth v 4)
          (nth v 5))))

  (fact "an evaluated game with an expired bomb hits the nearby player"
    (println "T012")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          bomb {:player-id :player-1, :timestamp ts-1}
          bomb-idx 4
          game (-> (d/make-game ts-1)
                   (assoc-in [:grid :v bomb-idx] {:bomb bomb}))
          {{v :v} :grid, players :players, :as evaluated-game} (g/eval game ts-2)
          bomb (:bomb (nth v bomb-idx))
          hit (:hit (:player-2 players))]
      evaluated-game =not=> game
      (:detonated bomb) => {:timestamp ts-2}
      (:hit (:player-1 players)) => nil?
      hit => some?
      (:timestamp hit) => ts-2
      (:player-id hit) => :player-1
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp ts-2, :player-id :player-1})
          ?cell
          (nth v 1)
          (nth v 3)
          (nth v 4)
          (nth v 5)
          (nth v 7))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 0)
          (nth v 2)
          (nth v 6)
          (nth v 8))))

  (fact "bomb detonations propagate to nearby bombs, leaving others unchanged"
    (println "T013")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 (/ config/bomb-timeout-ms 2))
          ts-3 (+ ts-1 config/bomb-timeout-ms)
          bomb-ts-1 {:player-id :player-1, :timestamp ts-1}
          bomb-ts-1-idx 6
          bomb-ts-2 {:player-id :player-1, :timestamp ts-2}
          bomb-ts-2-idx 8
          bomb-ts-2-iso-idx 4
          game (-> (d/make-game ts-1)
                   (assoc-in [:players :player-1 :bomb-radius] 3)
                   (assoc-in [:grid :v bomb-ts-1-idx] {:bomb bomb-ts-1})
                   (assoc-in [:grid :v bomb-ts-2-idx] {:bomb bomb-ts-2})
                   (assoc-in [:grid :v bomb-ts-2-iso-idx] {:bomb bomb-ts-2}))
          {{v :v} :grid, players :players, :as evaluated-game} (g/eval game ts-3)
          bomb-ts-1 (:bomb (nth v bomb-ts-1-idx))
          bomb-ts-2 (:bomb (nth v bomb-ts-2-idx))
          bomb-ts-2-iso (:bomb (nth v bomb-ts-2-iso-idx))]
      evaluated-game =not=> game
      (:detonated bomb-ts-1) => {:timestamp ts-3}
      (:detonated bomb-ts-2) => {:timestamp ts-3}
      (:detonated bomb-ts-2-iso) => nil?
      (:hit (:player-1 players)) => some?
      (get-in players [:player-1 :hit :player-id]) => :player-1
      (:hit (:player-2 players)) => nil?
      (:hit (:block (nth v 2))) => some?
      (tabular
        (fact "cells with fire"
          (:fire ?cell) => {:timestamp ts-3, :player-id :player-1})
          ?cell
          (nth v 0)
          (nth v 2)
          (nth v 3)
          (nth v 5)
          (nth v bomb-ts-1-idx)
          (nth v 7)
          (nth v bomb-ts-2-idx))
      (tabular
        (fact "cells without fire"
          (:fire ?cell) => nil?)
          ?cell
          (nth v 1)
          (nth v bomb-ts-2-iso-idx))))

  (fact "a player walking into a cell with fire gets hit"
    (println "T014")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          ts-3 (+ ts-2 (/ config/expiration-ms 2))
          bomb {:player-id :player-1, :timestamp ts-1}
          bomb-idx 3
          game (-> (d/make-game ts-1)
                   (assoc-in [:grid :v 4] nil)
                   (assoc-in [:grid :v bomb-idx] {:bomb bomb})
                   (g/eval ts-2)
                   (g/move :player-2 :down ts-3))
          players (:players game)
          hit-player-1 (:hit (:player-1 players))
          hit-player-2 (:hit (:player-2 players))]
      hit-player-1 => some?
      (:timestamp hit-player-1) => ts-2
      (:player-id hit-player-1) => :player-1
      hit-player-2 => some?
      (:timestamp hit-player-2) => ts-3
      (:player-id hit-player-2) => :player-1))
     
  (fact "at game evaluation, expired detonated bombs, fire and hit players get removed"
    (println "T015")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms 2)
          bomb {:player-id :player-1, :timestamp ts-1}
          bomb-idx 0
          game (-> (d/make-game ts-1)
                   (assoc-in [:players :player-1 :bomb-radius] 3)
                   (assoc-in [:grid :v bomb-idx] {:bomb bomb})
                   (g/eval ts-2)
                   (g/eval ts-3))
          {{v :v} :grid, players :players} game
          cell-0 (nth v bomb-idx)
          cell-1 (second v)
          cell-2 (nth v 2)]
      (:bomb cell-0) => nil?
      (:fire cell-0) => nil?
      (:player-id cell-0) => nil?
      (:player-id cell-1) => nil?
      (:block cell-2) => nil?
      (:coords (:player-1 players)) => nil?
      (:coords (:player-2 players)) => nil?))

  (fact "a game is over when less than 2 players are alive"
    (println "T016")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms)
          game (d/make-game ts-1)]
      (fact "last man standing wins"
        (println "T016.1")
        (let [game (-> game
                       (g/plant-bomb :player-1 ts-1)
                       (g/move :player-1 :down ts-1)
                       (g/move :player-1 :down ts-1)  ; pass block
                       (g/move :player-1 :right ts-1)
                       (g/eval ts-2)
                       (g/eval ts-3))
              {:keys [gameover players]} game]
          (count players) => 2
          (count (filter (comp nil? :coords second) players)) => 1
          (:winner gameover) => :player-1
          (:timestamp gameover) => ts-3))

      (fact "no winner on empty game"
        (println "T016.2")
        (let [game (-> game
                       (g/plant-bomb :player-1 ts-1)
                       (g/move :player-1 :down (d/make-timestamp))
                       (g/eval ts-2)
                       (g/eval ts-3))
              {:keys [gameover players]} game]
          (count players) => 2
          (count (filter (comp nil? :coords second) players)) => 2
          (:winner gameover) => nil?
          (:timestamp gameover) => ts-3))

      (fact "with 2 players, the game is ongoing"
        (println "T016.3")
        (let [game (g/eval game ts-2)
              {:keys [gameover players]} game]
          (count players) => 2
          (count (filter (comp nil? :coords second) players)) => 0
          gameover => nil?))))

  (fact "collecting a bomb item increases a player's bomb count"
    (println "T017")
    (let [ts (d/make-timestamp)
          bomb-item-cell (d/make-cell-item-bomb)
          bomb-item-cell-idx 3
          game (-> (d/make-game ts)
                   (assoc-in [:grid :v bomb-item-cell-idx] bomb-item-cell)
                   (g/eval ts))
          player-1 (get-in game [:players :player-1])]
      (:bomb-count player-1) => 1
      (let [game (-> game
                     (g/move :player-1 :down ts)
                     (g/eval ts))
            player-1 (get-in game [:players :player-1])]
        (:bomb-count player-1) => 2)))

  (fact "fire hits items, hit items disappear after a while"
    (println "T018")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms)
          bomb-item-cell (d/make-cell-item-bomb)
          bomb-item-cell-idx 3
          bomb {:player-id :player-1, :timestamp ts-1}
          bomb-idx 6
          game (-> (d/make-game ts-1)
                   (assoc-in [:grid :v bomb-item-cell-idx] bomb-item-cell)
                   (assoc-in [:grid :v bomb-idx :bomb] bomb)
                   (g/eval ts-2))
          game (g/eval game ts-2)
          {{v :v} :grid} game]
      (:hit (:item (nth v 3))) => {:timestamp ts-2}
      (let [game (g/eval game ts-3)
            {{v :v} :grid} game]
        (:item (nth v 3)) => nil?)))

  (fact "players who leave the game, die and are marked as :left"
    (println "T019")
    (let [ts (d/make-timestamp)
          game (-> (d/make-game ts)
                   (g/leave :player-1 ts))
          players (:players game)
          player-1 (:player-1 players)
          player-2 (:player-2 players)]
      (:hit player-1) => {:timestamp ts
                          :player-id :player-1}
      (:left player-1) => {:timestamp ts}
      (:hit player-2) => nil?
      (:left player-2) => nil?))

  (fact "players can leave a non-running game"
    (println "T020")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 1000)
          player (d/make-player :player-1)
          game (-> (d/make-empty-game)
                   (g/join player ts-1)
                   (g/leave :player-1 ts-2))
          players (:players game)]
      players => empty?))

  (fact "various player stats are collected"
    (println "T021")
    (let [ts-1 (d/make-timestamp)
          ts-2 (+ ts-1 config/bomb-timeout-ms)
          ts-3 (+ ts-2 config/expiration-ms)
          ts-4 (+ ts-3 3000)
          ts-5 (+ ts-4 config/expiration-ms)
          game (d/make-game ts-1)

          scenario-1 (-> game
                         (g/plant-bomb :player-1 ts-1)
                         (g/move :player-1 :down ts-1)
                         (g/move :player-1 :down ts-1)
                         (g/move :player-1 :down ts-1)  ; hits wall, shouldn't increase :moves
                         (g/eval ts-2)
                         (g/eval ts-3))

          scenario-1-1 (-> scenario-1
                           (g/next-round ts-4)
                           (g/eval ts-4))

          scenario-1-2 (-> scenario-1-1
                           (g/leave :player-2 ts-4)
                           (g/eval ts-4)
                           (g/eval ts-5))

          scenario-1-3 (-> scenario-1-2
                           (g/next-round ts-5)
                           (g/eval ts-5))

          scenario-2 (-> game
                         (g/plant-bomb :player-1 ts-1)
                         (g/move :player-1 :down ts-1)
                         (g/eval ts-2)
                         (g/eval ts-3))

          stats-1 (:stats scenario-1)
          s1-r1 (get-in stats-1 [:round])
          s1-r-p1 (get-in stats-1 [:round :players :player-1])
          s1-r-p2 (get-in stats-1 [:round :players :player-2])
          s1-a-p1 (get-in stats-1 [:all :players :player-1])
          s1-a-p2 (get-in stats-1 [:all :players :player-2])

          stats-1-1 (:stats scenario-1-1)
          s11-r1 (get-in stats-1-1 [:round])
          s11-r-p1 (get-in stats-1-1 [:round :players :player-1])
          s11-r-p2 (get-in stats-1-1 [:round :players :player-2])
          s11-a-p1 (get-in stats-1-1 [:all :players :player-1])
          s11-a-p2 (get-in stats-1-1 [:all :players :player-2])

          stats-1-2 (:stats scenario-1-2)
          s12-r1 (get-in stats-1-2 [:round])
          s12-r-p1 (get-in stats-1-2 [:round :players :player-1])
          s12-r-p2 (get-in stats-1-2 [:round :players :player-2])
          s12-a-p1 (get-in stats-1-2 [:all :players :player-1])
          s12-a-p2 (get-in stats-1-2 [:all :players :player-2])

          stats-1-3 (:stats scenario-1-3)
          s13-r-p2 (get-in stats-1-3 [:round :players :player-2])
          s13-a-p2 (get-in stats-1-3 [:all :players :player-2])

          stats-2 (:stats scenario-2)
          s2-r1 (get-in stats-2 [:round])
          s2-r-p1 (get-in stats-2 [:round :players :player-1])
          s2-r-p2 (get-in stats-2 [:round :players :player-2])
          s2-a-p1 (get-in stats-2 [:all :players :player-1])
          s2-a-p2 (get-in stats-2 [:all :players :player-2])]

      (println "s1-r")
      (:started-at s1-r1) => ts-1
      (:duration s1-r1) => (- ts-3 ts-1)
      s1-r-p1 => {:kills 1
                  :death? false
                  :suicide? false
                  :moves 2
                  :items {:bomb 0
                          :fire 0}}
      s1-r-p2 => {:kills 0
                  :death? true
                  :suicide? false
                  :moves 0
                  :items {:bomb 0
                          :fire 0}}
      (println "s1-a")
      s1-a-p1 => {:joined-at ts-1
                  :playing-time (- ts-3 ts-1)
                  :kills 1
                  :deaths 0
                  :suicides 0
                  :wins 1
                  :moves 2
                  :items {:bomb 0
                          :fire 0}}
      s1-a-p2 => {:joined-at ts-1
                  :playing-time (- ts-3 ts-1)
                  :kills 0
                  :deaths 1
                  :suicides 0
                  :wins 0
                  :moves 0
                  :items {:bomb 0
                          :fire 0}}

      (println "s11-r")
      (:started-at s11-r1) => ts-4
      (:duration s11-r1) => 0
      s11-r-p1 => {:kills 0
                   :death? false
                   :suicide? false
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}
      s11-r-p2 => {:kills 0
                   :death? false
                   :suicide? false
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}
      (println "s11-a")
      s11-a-p1 => {:joined-at ts-1
                   :playing-time (- ts-3 ts-1)
                   :kills 1
                   :deaths 0
                   :suicides 0
                   :wins 1
                   :moves 2
                   :items {:bomb 0
                           :fire 0}}
      s11-a-p2 => {:joined-at ts-1
                   :playing-time (- ts-3 ts-1)
                   :kills 0
                   :deaths 1
                   :suicides 0
                   :wins 0
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}

      (println "s12-r")
      (:started-at s12-r1) => ts-4
      (:duration s12-r1) => (- ts-5 ts-4)
      s12-r-p1 => {:kills 0
                   :death? false
                   :suicide? false
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}
      s12-r-p2 => {:kills 0
                   :death? true
                   :suicide? true
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}
      (println "s12-a")
      s12-a-p1 => {:joined-at ts-1
                   :playing-time (+ (- ts-3 ts-1) (- ts-5 ts-4))
                   :kills 1
                   :deaths 0
                   :suicides 0
                   :wins 2
                   :moves 2
                   :items {:bomb 0
                           :fire 0}}
      s12-a-p2 => {:joined-at ts-1
                   :playing-time(+ (- ts-3 ts-1) (- ts-5 ts-4))
                   :kills 0
                   :deaths 2
                   :suicides 1
                   :wins 0
                   :moves 0
                   :items {:bomb 0
                           :fire 0}}
      
      (println "s13-a")
      s13-r-p2 => nil?
      s13-a-p2 => nil?

      (println "s2-r")
      (:started-at s2-r1) => ts-1
      (:duration s2-r1) => (- ts-3 ts-1)
      s2-r-p1 => {:kills 1
                  :death? true
                  :suicide? true
                  :moves 1
                  :items {:bomb 0
                          :fire 0}}
      s2-r-p2 => {:kills 0
                  :death? true
                  :suicide? false
                  :moves 0
                  :items {:bomb 0
                          :fire 0}}
      (println "s2-a")
      s2-a-p1 => {:joined-at ts-1
                  :playing-time (- ts-3 ts-1)
                  :kills 1
                  :deaths 1
                  :suicides 1
                  :wins 0
                  :moves 1
                  :items {:bomb 0
                          :fire 0}}
      s2-a-p2 => {:joined-at ts-1
                  :playing-time (- ts-3 ts-1)
                  :kills 0
                  :deaths 1
                  :suicides 0
                  :wins 0
                  :moves 0
                  :items {:bomb 0
                          :fire 0}}))
)
