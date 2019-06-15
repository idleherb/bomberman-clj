(ns bomberman-clj.client.components.lobby.player-dialog
  (:require [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.state :as s]))

(defn- close-dialog
  []
  (swap! s/state assoc-in [:app :player-dialog :open?] false))

(defn player-dialog
  [game-id name]
  [:div {:class "modal"}
   [:div {:class "game-dialog"}
    [:label {:for "in-name"} "Player name"
     [:input {:name "in-name"
              :value name
              :type "text"
              :required true
              :on-change #(swap! s/state assoc-in [:app :player-dialog :name] (-> % .-target .-value))}]]
    [:div {:class "buttons"}
     [:button {:class "secondary"
               :on-click close-dialog} "Cancel"]
     [:button {:class "primary"
               :disabled (empty? name)
               :on-click (fn [] (a/join game-id name) (close-dialog))} "Join Game"]]]])
