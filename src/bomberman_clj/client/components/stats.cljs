(ns bomberman-clj.client.components.stats
  (:require [bomberman-clj.client.components.player-stats :refer [player-stats]
                                                          :rename {player-stats el-player-stats}]
            [bomberman-clj.client.util :as util]))

(defn stats [stats-state players]
  (let [sorted-stats (-> stats-state
                         (util/accumulate-stats)
                         (util/sorted-stats))
        duration (get-in sorted-stats [:round :duration])
        all-players-stats (get-in sorted-stats [:all :players])]
    [:div {:class "stats"}
      [:h4 [:span {:class "time"} (util/format-time duration)]]
      [:ol (for [[player-id player-stats] all-players-stats]
        (let [player-name (get-in players [player-id :name])]
          [:li {:key player-id}
            [el-player-stats player-stats player-name]]))]]))
