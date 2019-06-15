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
  [name width height num-players]
  (async/go
    (async/>! actions-ch {:type :open
                          :payload {:name name
                                    :width width
                                    :height height
                                    :num-players num-players}})))

(defn close
  [game-id]
  (async/go
    (async/>! actions-ch {:type :close
                          :payload game-id})))
