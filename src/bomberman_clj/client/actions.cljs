(ns bomberman-clj.client.actions
  (:require [cljs.core.async :as async
                             :include-macros true]
            [bomberman-clj.client.state :as s]))

(defonce actions-ch (get-in @s/state [:app :actions-ch]))

(defn action
  [payload]
  (async/go
    (async/>! actions-ch {:type :action, :payload payload})))

(defn join
  [game-id player-name]
  (async/go
    (async/>! actions-ch {:type :join
                          :payload {:game-id game-id
                                    :player-name player-name}})))

(defn leave
  []
  (async/go
    (async/>! actions-ch {:type :leave})))

(defn open
  [game-name]
  (async/go
    (async/>! actions-ch {:type :open
                          :payload {:name game-name
                                    :width 17
                                    :height 15
                                    :num-players 2}})))

(defn close
  [game-id]
  (async/go
    (async/>! actions-ch {:type :close
                          :payload game-id})))
