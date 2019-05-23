(ns bomberman-clj.client.components.player-form
  (:require [bomberman-clj.client.components.num-players :refer [num-players]]
            [bomberman-clj.client.actions :as actions]
            [bomberman-clj.client.state :as s]))

(defn player-form [state]
  [:div
    [:div {:class "name"}
      [:label {:for "in-name"} "NAME"
      [:input {:class "in-name"
               :name "in-name"
               :type "text"
               :required true
               :on-change #(swap! s/state assoc-in [:app :player-name] (-> % .-target .-value))}]]]
    [:div
      [num-players state]
      [:button {:on-click #(actions/join (get-in state [:app :player-name]))} "Join"]
      [:button {:on-click actions/leave} "Leave"]]])
