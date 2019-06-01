(ns bomberman-clj.client.components.player-form
  (:require [bomberman-clj.client.components.num-players :refer [num-players]
                                                         :rename {num-players el-num-players}]
            [bomberman-clj.client.actions :as actions]
            [bomberman-clj.client.state :as s]))

(defn player-form [player-name
                   cur-num-players
                   max-num-players]
  [:div
    [:div {:class "name"}
      [:label {:for "in-name"} "NAME"
      [:input {:class "in-name"
               :name "in-name"
               :type "text"
               :required true
               :on-change #(swap! s/state assoc-in [:app :player-name] (-> % .-target .-value))}]]]
    [:div
      [el-num-players cur-num-players max-num-players]
      [:button {:on-click #(actions/join player-name)} "Join"]
      [:button {:on-click actions/leave} "Leave"]]])
