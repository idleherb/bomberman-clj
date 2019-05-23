(ns bomberman-clj.client.components.game
  (:require [bomberman-clj.client.components.cell :refer [cell]
                                                  :rename {cell el-cell}]
            [bomberman-clj.client.components.num-players :refer [num-players]
                                                         :rename {num-players el-num-players}]
            [bomberman-clj.client.state :as s]))

(defn- el-gameover [state]
  [:div {:class "gameover"}
    (if-let [winner-id (get-in state [:game :gameover :winner])]
      (let [players (get-in state [:game :players])
            name (:name (get players winner-id))]
        (str name " wins!"))
      "no winner")])

(defn- el-game [state]
  (let [{:keys [players grid height width]} (:game state)]
    [:div {:class "game"}
      (for [row (range height)]
        [:div {:class "row" :key (str "row-" row)}
          (for [col (range width)]
            (let [cell-idx (+ (* row width) col)
                  cell (nth (:v grid) cell-idx)]
              ^{:key (str "cell-" col row)}
              [el-cell cell players]))])]))

(defn game [state]
  (let [{:keys [gameover in-progress?]} (:game state)]
    (when in-progress?
      [:div
        (if gameover
          [el-gameover state]
          [el-game state])
        (when-not gameover
          [:div {:style {:font-size "20px"
                         :margin "10px"
                         :display "flex"
                         :justify-content "center"}}
            [el-num-players state]])])))