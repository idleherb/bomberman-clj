(ns bomberman-clj.client.components.game.core
  (:require
   [bomberman-clj.client.actions :as a]
   [bomberman-clj.client.components.game.gameover :refer [gameover]
                                                  :rename {gameover el-gameover}]
   [bomberman-clj.client.components.game.grid :refer [grid]
                                              :rename {grid el-grid}]
   [bomberman-clj.client.components.game.stats :refer [stats]
                                               :rename {stats el-stats}]))

(defn game [game-state]
  (let [{:keys [gameover
                grid
                num-players
                players
                in-progress?
                stats]} game-state
        cur-num-players (count players)]
    (cond
      gameover [el-gameover gameover players]
      (not in-progress?) [:div {:class "col"}
                          [:div {:class "row"}
                           [:span {:class "emoji hourglass"}]
                           (str "Waiting for other players to join (" cur-num-players "/" num-players ")")]
                          [:div {:class "buttons"}
                           [:button {:class "secondary"
                                    :on-click a/leave} "Leave game"]]]
      :else [:div {:class "col"}
             [el-stats stats players]
             [el-grid grid players]
             [:div {:class "buttons"}
              [:button {:class "secondary"
                        :on-click a/leave} "Leave game"]]])))
