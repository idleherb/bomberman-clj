(ns bomberman-clj.client.components.game
  (:require [bomberman-clj.client.components.gameover :refer [gameover]
                                                      :rename {gameover el-gameover}]
            [bomberman-clj.client.components.grid :refer [grid]
                                                  :rename {grid el-grid}]
            [bomberman-clj.client.components.num-players :refer [num-players]
                                                         :rename {num-players el-num-players}]
            [bomberman-clj.client.components.stats :refer [stats]
                                                   :rename {stats el-stats}]))

(defn game [game-state]
  (let [{:keys [gameover players]} game-state]
    (if gameover
      [el-gameover gameover players]
      (let [{:keys [grid num-players stats]} game-state
            cur-num-players (count players)]
        [:div {:class "col"}
          [el-stats stats players]
          [el-grid grid players]
          [el-num-players cur-num-players num-players]]))))
