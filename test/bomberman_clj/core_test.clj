(ns bomberman-clj.core-test
  (:require [clojure.core.async :as async]
            [midje.sweet :refer [fact facts => =not=> tabular]]
            [bomberman-clj.core :refer [game-loop]]
            [bomberman-clj.test-data :refer [make-timestamp]]))

(facts "about the game loop"
  (fact "the game state is evaluated when a :refresh event is sent"
    (let [plr {:player-1 {:glyph \P, :bomb-count 1}}
          v [nil nil nil
            nil plr nil
            nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          ch-in (async/chan)
          ch-out (async/chan)]
      (game-loop arena ch-in ch-out)
      (async/put! ch-in {:timestamp (make-timestamp), :type :refresh})
      (async/<!! ch-out) => {:state (assoc arena :gameover {:timestamp (make-timestamp)
                                                            :winner :player-1})}
      (async/close! ch-in)))

  (fact "player actions are executed when a :action event is sent"
    (let [plr {:player-1 {:glyph \P, :bomb-count 1}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 {:x 1, :y 1}}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
          ch-in (async/chan)
          ch-out (async/chan)]
      (game-loop arena ch-in ch-out)
      (async/put! ch-in {:timestamp (make-timestamp)
                         :type :action
                         :action :plant-bomb
                         :player-id :player-1})
      (async/put! ch-in {:timestamp (make-timestamp)
                         :type :action
                         :action :move
                         :player-id :player-1
                         :payload :east})
      (async/put! ch-in {:timestamp (make-timestamp)
                         :type :action
                         :action :move
                         :player-id :player-1
                         :payload :south})
      (async/put! ch-in {:timestamp (make-timestamp), :type :refresh})
      (async/<!! ch-out) => {:state {
        :bombs {:bomb-x1y1 {:x 1 :y 1}}
        :gameover {:timestamp 1552767537306 :winner :player-1}
        :grid {:height 3
               :v [nil nil nil
                   nil {:bomb-x1y1 {:player-id :player-1 :timestamp 1552767537306}} nil
                   nil nil {:player-1 {:bomb-count 0 :glyph \P}}]
               :width 3}
        :players {:player-1 {:x 2 :y 2}}}}
      (async/close! ch-in)))
)
