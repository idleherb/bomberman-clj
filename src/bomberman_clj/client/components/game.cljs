(ns bomberman-clj.client.components.game
  (:require
   [bomberman-clj.client.actions :as a]
   [bomberman-clj.client.components.gameover :refer [gameover]
                                             :rename {gameover el-gameover}]
   [bomberman-clj.client.components.grid :refer [grid]
                                         :rename {grid el-grid}]
   [bomberman-clj.client.components.num-players :refer [num-players]
                                                :rename {num-players el-num-players}]
   [bomberman-clj.client.components.stats :refer [stats]
                                          :rename {stats el-stats}]))

(defn game [game-state]
  (let [{:keys [gameover
                grid
                num-players
                num-spectators
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
             [el-num-players cur-num-players num-players num-spectators]
             [:button {:class "secondary"
                       :on-click a/leave} "Leave game"]])))
