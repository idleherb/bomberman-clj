(ns bomberman-clj.client.components.lobby.core
  (:require
   [bomberman-clj.client.state :as s]
   [bomberman-clj.client.components.lobby.game-dialog :refer [game-dialog]
                                                      :rename {game-dialog el-game-dialog}]
   [bomberman-clj.client.components.lobby.game-entry :refer [game-entry]
                                                     :rename {game-entry el-game-entry}]
   [bomberman-clj.client.components.lobby.player-dialog :refer [player-dialog]
                                                        :rename {player-dialog el-player-dialog}]))

(defn- open-game-dialog
  []
  (swap! s/state assoc-in [:app :game-dialog :open?] true))

(defn lobby
  [lobby-state game-dialog-state player-dialog-state]
  (let [{game-dialog-open? :open?, game-name :name
         :keys [width height num-players]} game-dialog-state
        {player-dialog-open? :open?
         game-id :game-id
         player-name :name} player-dialog-state]
    [:div {:class "col lobby"}
     [:div {:class "row flex-end"} (when (empty? lobby-state)
             "No open games, start a new one here")
      [:button {:class "button--emoji"
                :on-click open-game-dialog} "➕"]]
     (when game-dialog-open?
       [el-game-dialog game-name width height num-players])
     (when player-dialog-open?
       [el-player-dialog game-id player-name])
     (for [game lobby-state]
       (let [{:keys [game-id name width height num-players cur-num-players admin?]} game]
         ^{:key (str "game-" (:game-id game))}
         [el-game-entry game-id name width height num-players cur-num-players admin?]))]))
