(ns bomberman-clj.client.components.player-stats)

(defn player-stats [player-stats-state player-name]
  (let [{:keys [wins deaths suicides kills moves]} player-stats-state]
    [:ul {:class "player-stats"}
     [:li {:class "attr"}
      [:span {:class "attr__val"} player-name]]
     [:li {:class "attr"}
      [:span {:class "attr__name"} "Wins"]
      [:span {:class "attr__val attr__val tiny"} wins]]
     [:li {:class "attr"}
      [:span {:class "attr__name"} "Kills"]
      [:span {:class "attr__val attr__val tiny"} kills]]
     [:li {:class "attr"}
      [:span {:class "attr__name"} "Deaths"]
      [:span {:class "attr__val attr__val tiny"} deaths]]
     [:li {:class "attr"}
      [:span {:class "attr__name"} "Suicides"]
      [:span {:class "attr__val attr__val tiny"} suicides]]
     [:li {:class "attr"}
      [:span {:class "attr__name"} "Steps"]
      [:span {:class "attr__val attr__val tiny"} moves]]]))
