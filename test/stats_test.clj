(ns bomberman-clj.game-test
  (:require [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.game :as g]
            [bomberman-clj.config :as config]
            [bomberman-clj.test-data :as d]))

(facts "about game stats"
  (fact "various player stats are collected"
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
                             (g/eval ts-2)  ; shouldn't increase :kills again
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
          s1-r-p1 => {:won? true
                      :dead? false
                      :suicide? false
                      :moves 2
                      :kills 1
                      :items {:bomb 0
                              :fire 0}}
          s1-r-p2 => {:won? false
                      :dead? true
                      :suicide? false
                      :moves 0
                      :kills 0
                      :items {:bomb 0
                              :fire 0}}
          (println "s1-a")
          s1-a-p1 => {:joined-at ts-1
                      :playing-time 0
                      :kills 0
                      :deaths 0
                      :suicides 0
                      :wins 0
                      :moves 0
                      :items {:bomb 0
                              :fire 0}}
          s1-a-p2 => {:joined-at ts-1
                      :playing-time 0
                      :kills 0
                      :deaths 0
                      :suicides 0
                      :wins 0
                      :moves 0
                      :items {:bomb 0
                              :fire 0}}

          (println "s11-r")
          (:started-at s11-r1) => ts-4
          (:duration s11-r1) => 0
          s11-r-p1 => {:won? false
                       :dead? false
                       :suicide? false
                       :moves 0
                       :kills 0
                       :items {:bomb 0
                               :fire 0}}
          s11-r-p2 => {:won? false
                       :dead? false
                       :suicide? false
                       :moves 0
                       :kills 0
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
          (:duration s12-r1) => config/expiration-ms
          s12-r-p1 => {:won? true
                       :dead? false
                       :suicide? false
                       :moves 0
                       :kills 0
                       :items {:bomb 0
                               :fire 0}}
          s12-r-p2 => {:won? false
                       :dead? true
                       :suicide? true
                       :moves 0
                       :kills 0
                       :items {:bomb 0
                               :fire 0}}
          (println "s12-a")
          s12-a-p1 => {:joined-at ts-1
                       :playing-time (- ts-3 ts-1)
                       :kills 1
                       :deaths 0
                       :suicides 0
                       :wins 1
                       :moves 2
                       :items {:bomb 0
                               :fire 0}}
          s12-a-p2 => {:joined-at ts-1
                       :playing-time (- ts-3 ts-1)
                       :kills 0
                       :deaths 1
                       :suicides 0
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
          s2-r-p1 => {:won? false
                      :dead? true
                      :suicide? true
                      :moves 1
                      :kills 1
                      :items {:bomb 0
                              :fire 0}}
          s2-r-p2 => {:won? false
                      :dead? true
                      :suicide? false
                      :moves 0
                      :kills 0
                      :items {:bomb 0
                              :fire 0}}
          (println "s2-a")
          s2-a-p1 => {:joined-at ts-1
                      :playing-time 0
                      :kills 0
                      :deaths 0
                      :suicides 0
                      :wins 0
                      :moves 0
                      :items {:bomb 0
                              :fire 0}}
          s2-a-p2 => {:joined-at ts-1
                      :playing-time 0
                      :kills 0
                      :deaths 0
                      :suicides 0
                      :wins 0
                      :moves 0
                      :items {:bomb 0
                              :fire 0}}))
)