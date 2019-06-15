(ns bomberman-clj.client.components.app
  (:require [reagent.core :as r]
            [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.components.game :refer [game]
                                                  :rename {game el-game}]
            [bomberman-clj.client.components.lobby.core :refer [lobby]
                                                        :rename {lobby el-lobby}]
            [bomberman-clj.client.components.volume :refer [volume]
                                                    :rename {volume el-volume}]
            [bomberman-clj.client.state :as s]
            [bomberman-clj.client.style :as style]))

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

(defn update-style! [state]
  (let [{:keys [app game]} state
        stale-style? (:stale-style? app)
        gameover? (some? (:gameover game))]
    (cond
      (and stale-style? (not gameover?)) (do
        (swap! s/state assoc-in [:app :soft-block] (style/soft-block-emoji))
        (swap! s/state assoc-in [:app :stale-style?] false))
      (and gameover? (not stale-style?)) (swap! s/state assoc-in [:app :stale-style?] true)
      )))

(defn app []
  (r/create-class
   {:component-did-update
    #(when (get-in @s/state [:game :in-progress?])
       (.focus (r/dom-node %1)))
    :reagent-render
    (fn []
      (let [{app-state :app, :keys [game lobby], :as state} @s/state]
        (update-style! state)
        [:div {:class "app"
               :tabIndex 0
               :on-key-down #(on-key-down (.-keyCode %))}
         [el-volume (:mute? app-state)]
         (if (some? lobby)
           [el-lobby lobby
            (:game-dialog app-state)
            (:player-dialog app-state)]
           [el-game game])]))}))
