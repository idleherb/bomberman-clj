(ns bomberman-clj.stats
  (:require [bomberman-clj.specs :as specs]))

(defn inc-player-moves
  [stats player-id]
  (-> stats
      (update-in [:round :players player-id :moves] inc)
      (update-in [:all :players player-id :moves] inc)))

(defn init-player-stats
  [stats player-id timestamp]
  {:pre [(specs/valid? ::specs/stats stats)
         (specs/valid? ::specs/player-id player-id)
         (specs/valid? ::specs/timestamp timestamp)]
   :post [(specs/valid? ::specs/stats %)]}
  (let [player-round-stats {:kills 0
                            :death? false
                            :suicide? false
                            :moves 0
                            :items {:bomb 0
                                    :fire 0}}
        player-all-stats {:joined-at nil
                          :playing-time 0
                          :kills 0
                          :deaths 0
                          :suicides 0
                          :wins 0
                          :moves 0
                          :items {:bomb 0
                                  :fire 0}}]
    (-> stats
        (assoc-in [:round player-id] player-round-stats)
        (assoc-in [:all player-id] player-all-stats))))

(defn- update-playing-time
  [playing-time old-duration new-duration]
  (+ new-duration (- playing-time old-duration)))

(defn update-time
  [stats timestamp]
  (let [old-duration (get-in stats [:round :duration])
        new-duration (- timestamp (get-in stats [:round :started-at]))
        new-stats (assoc-in stats [:round :duration] new-duration)
        new-stats (update-in new-stats [:all :players]
          (fn [players]
            (into {} (map (fn [[id stats]]
                            [id (update-in stats
                                           [:playing-time]
                                           #(update-playing-time % old-duration new-duration))])
                          players))))]
    new-stats))
