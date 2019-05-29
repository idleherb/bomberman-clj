(ns bomberman-clj.client.components.stats
  (:require [bomberman-clj.client.util :as util]
            [bomberman-clj.client.components.player-stats
             :refer [player-stats]
             :rename {player-stats el-player-stats}]))

(defn stats [state]
  (let [game-stats (-> (get-in state [:game :stats])
                       (util/accumulate-stats)
                       (util/sorted-stats))
        duration (get-in game-stats [:round :duration])
        all-players-stats (get-in game-stats [:all :players])]
    [:div {:class "stats"}
     [:h4 [:span {:class "time"} (util/format-time duration)]]
     [:ol
      (for [entry all-players-stats]
        (let [player-id (first entry)
              player-name (get-in state [:game :players player-id :name])]
          [:li {:key player-id}
           [el-player-stats player-name (second entry)]]))]]))
