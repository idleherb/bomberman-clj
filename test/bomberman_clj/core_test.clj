(ns bomberman-clj.core-test
  (:require [clojure.core.async :as async]
            [midje.sweet :refer [future-fact facts =>]]
            [bomberman-clj.core :as c]
            [bomberman-clj.test-data :as d]))

(facts "about the game loop"
  (future-fact "the game state is evaluated when a :refresh event is sent"
    (let [ch-in (async/chan)
          ch-out (async/chan)
          timestamp (d/make-timestamp)]
      (c/game-loop 3 3 ch-in ch-out)
      (async/put! ch-in {:timestamp timestamp, :type :refresh})
      (async/<!! ch-out) => {:timestamp timestamp
                             :state (assoc arena :gameover {:timestamp timestamp
                                                            :winner :player-1})}
      (async/close! ch-in)))

  (future-fact "player actions are executed when a :action event is sent"
    (let [ch-in (async/chan)
          ch-out (async/chan)
          timestamp (d/make-timestamp)]
      (c/game-loop 3 3 ch-in ch-out)
      (async/put! ch-in {:timestamp timestamp
                         :type :action
                         :action :plant-bomb
                         :player-id :player-1})
      (async/put! ch-in {:timestamp timestamp
                         :type :action
                         :action :move
                         :player-id :player-1
                         :payload :right})
      (async/put! ch-in {:timestamp timestamp
                         :type :action
                         :action :move
                         :player-id :player-1
                         :payload :down})
      (async/put! ch-in {:timestamp timestamp, :type :refresh})
      (async/<!! ch-out) => {:timestamp timestamp
                             :state {
        :gameover {:timestamp 1552767537306 :winner :player-1}
        :grid {:height 3
               :v [nil nil nil
                   nil {:bomb {:player-id :player-1, :timestamp timestamp}} nil
                   nil nil (assoc plr :player
                             (assoc (:player plr) :bomb-count
                               (dec (:bomb-count (:player plr)))))]
               :width 3}
        :players {:player-1 {:x 2 :y 2}}}}
      (async/close! ch-in)))
)
