(ns bomberman-clj.client.actions
  (:require [cljs.core.async :as async
                             :include-macros true]
            [bomberman-clj.client.state :as s]))

(defn- actions-ch
  []
  (get-in @s/state [:app :actions-ch]))

(defn action
  [payload]
  (async/go
    (async/>! (actions-ch) {:type :action, :payload payload})))

(defn join
  [player-name]
  (async/go
    (async/>! (actions-ch) {:type :join
                            :payload {:name player-name}})))

(defn leave
  []
  (async/go
    (async/>! (actions-ch) {:type :leave})))
