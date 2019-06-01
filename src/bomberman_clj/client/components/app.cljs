(ns bomberman-clj.client.components.app
  (:require [bomberman-clj.client.actions :as actions]
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
        38 (actions/action {:action :move, :direction :up})
        40 (actions/action {:action :move, :direction :down})
        37 (actions/action {:action :move, :direction :left})
        39 (actions/action {:action :move, :direction :right})
        32 (actions/action {:action :plant-bomb})
        13 (actions/action {:action :detonate-bombs})
        nil))))

(defn app []
  (let [{app-state :app, game :game} @s/state]
    [:div {:class "app"
           :tabIndex 0
           :on-key-down #(on-key-down (.-keyCode %))}
      [el-volume (:mute? app-state)]
      (if (:in-progress? game)
        [el-game game]
        (let [player-name (:player-name app-state)
              cur-num-players (count (:players game))
              max-num-players (:num-players game)]
          [el-player-form player-name
                          cur-num-players
                          max-num-players]))]))
