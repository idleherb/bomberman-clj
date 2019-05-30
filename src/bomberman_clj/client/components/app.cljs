(ns bomberman-clj.client.components.app
  (:require [bomberman-clj.client.actions :as actions]
            [bomberman-clj.client.components.game :refer [game]]
            [bomberman-clj.client.components.player-form :refer [player-form]]
            [bomberman-clj.client.state :as s]))

(defn on-key-down [code]
  (let [state @s/state
        in-progress? (get-in state [:game :in-progress?])]
    (when in-progress?
      (condp = code
        38 (actions/action {:action :move, :direction :up})
        40 (actions/action {:action :move, :direction :down})
        37 (actions/action {:action :move, :direction :left})
        39 (actions/action {:action :move, :direction :right})
        32 (actions/action {:action :plant-bomb})
        13 (actions/action {:action :detonate-bombs})
        nil))))

(defn app []
  (let [state @s/state
        in-progress? (get-in state [:game :in-progress?])]
    [:div {:class "app"
           :tabIndex 0
           :on-key-down #(on-key-down (.-keyCode %))}
      (if in-progress?
        [game state]
        [player-form state])]))
