(ns bomberman-clj.client.components.lobby.game-entry
  (:require [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.state :as s]))

(defn- open-player-dialog
  [game-id]
  (let [player-dialog (get-in @s/state [:app :player-dialog])]
    (swap! s/state assoc-in [:app :player-dialog]
           (assoc player-dialog :open? true
                                :game-id game-id))))

(defn game-entry
  [game-id
   name
   width
   height
   num-players
   cur-num-players
   admin?]
  [:ul {:class "game-entry"}
    [:li {:class "attr"}
      [:span {:class "attr__val long"} name]]
    [:li {:class "attr"}
      [:span {:class "attr__name"} "Size"]
      [:span {:class "attr__val attr__val small"} (str width "x" height)]]
    [:li {:class "attr"}
      [:span {:class "attr__name"} "Players"]
      [:span {:class "attr__val attr__val small"} (str cur-num-players "/" num-players)]]
    [:li [:button {:class ["emoji"]
                   :disabled (= cur-num-players num-players)
                   :on-click #(open-player-dialog game-id)}
     (if (< cur-num-players num-players) "🎮" "👁️")]]
    (when admin?
      [:li [:button {:class ["emoji"]
                     :on-click #(a/close game-id)} "🗑️"]])])
