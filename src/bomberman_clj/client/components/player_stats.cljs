(ns bomberman-clj.client.components.player-stats)

(defn player-stats
  [player-name stats]
  (let [{:keys [wins deaths suicides kills moves]} stats]
    [:ul {:class "player-stats"}
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__val"} player-name]]
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__name"} "Wins"]
      [:span {:class "stats-attr__val stats-attr__val--num"} wins]]
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__name"} "Kills"]
      [:span {:class "stats-attr__val stats-attr__val--num"} kills]]
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__name"} "Deaths"]
      [:span {:class "stats-attr__val stats-attr__val--num"} deaths]]
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__name"} "Suicides"]
      [:span {:class "stats-attr__val stats-attr__val--num"} suicides]]
     [:li {:class "stats-attr"}
      [:span {:class "stats-attr__name"} "Steps"]
      [:span {:class "stats-attr__val stats-attr__val--num"} moves]]]))
