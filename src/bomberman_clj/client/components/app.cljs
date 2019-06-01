(ns bomberman-clj.client.components.app
  (:require [reagent.core :as r]
            [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.components.game :refer [game]
                                                  :rename {game el-game}]
            [bomberman-clj.client.components.player-form :refer [player-form]
                                                         :rename {player-form el-player-form}]
            [bomberman-clj.client.components.volume :refer [volume]
                                                    :rename {volume el-volume}]
            [bomberman-clj.client.state :as s]))

(defn on-key-down [code]
  (let [state @s/state
        in-progress? (get-in state [:game :in-progress?])]
    (when in-progress?
      (condp = code
        38 (a/action {:action :move, :direction :up})
        40 (a/action {:action :move, :direction :down})
        37 (a/action {:action :move, :direction :left})
        39 (a/action {:action :move, :direction :right})
        32 (a/action {:action :plant-bomb})
        13 (a/action {:action :detonate-bombs})
        nil))))

(defn app []
  (r/create-class
   {:component-did-update #(when (get-in @s/state [:game :in-progress?])
                             (.focus (r/dom-node %1)))
    :reagent-render (fn []
                      (let [{app-state :app, game :game} @s/state]
                        [:div {:class "app"
                               :tabIndex 0
                               :on-key-down #(on-key-down (.-keyCode %))}
                         [el-volume (:mute? app-state)]
                         (if (:in-progress? game)
                           [el-game game]
                           (let [player-name (:player-name app-state)
                                 cur-num-players (count (:players game))
                                 {:keys [num-players num-spectators]} game]
                             [el-player-form player-name
                                             cur-num-players
                                             num-players
                                             num-spectators]))]))}))
