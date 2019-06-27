(ns bomberman-clj.domain.game.stats
  (:require [bomberman-clj.domain.game.specs :as s]))

(def round-player-stats {:won? false
                         :dead? false
                         :suicide? false
                         :moves 0
                         :kills 0
                         :items {:bomb 0
                                 :fire 0}})

(defn add-kill
  [stats killer-id corpse-id]
  (-> stats
      (assoc-in  [:round :players corpse-id :dead?] true)
      (update-in [:round :players killer-id :kills] inc)))

(defn add-move
  [stats player-id]
  (-> stats
      (update-in [:round :players player-id :moves] inc)))

(defn add-suicide
  [stats player-id]
  (-> stats
      (assoc-in [:round :players player-id :dead?]    true)
      (assoc-in [:round :players player-id :suicide?] true)))

(defn add-win
  [stats player-id]
  (-> stats
      (assoc-in [:round :players player-id :won?] true)))

(defn- update-player-history
  [stats player-id]
  (let [duration (get-in stats [:round :duration])
        round    (get-in stats [:round :players player-id])
        history  (get-in stats [:all   :players player-id])]
    (cond-> history
      true (update-in [:playing-time] + duration)
      true (update-in [:moves] + (:moves round))
      true (update-in [:kills] + (:kills round))
      (:won?     round) (update-in [:wins]     inc)
      (:dead?    round) (update-in [:deaths]   inc)
      (:suicide? round) (update-in [:suicides] inc))))

(defn reset-round
  [stats timestamp]
  (-> stats
      (update-in [:all :players]
                 #(into {} (map (fn [[k _]] [k (update-player-history stats k)])
                                %)))
      (assoc-in  [:round :started-at] timestamp)
      (assoc-in  [:round :duration] 0)
      (update-in [:round :players]
                 #(into {} (map (fn [[k _]] [k round-player-stats])
                                %)))))

(defn init-player-stats
  [stats player-id timestamp]
  {:pre [(s/valid? ::s/stats     stats)
         (s/valid? ::s/player-id player-id)
         (s/valid? ::s/timestamp timestamp)]
   :post [(s/valid? ::s/stats %)]}
  (let [all-player-stats {:joined-at timestamp
                          :playing-time 0
                          :kills 0
                          :deaths 0
                          :suicides 0
                          :wins 0
                          :moves 0
                          :items {:bomb 0
                                  :fire 0}}]
    (-> stats
        (assoc-in [:round :players player-id] round-player-stats)
        (assoc-in [:all   :players player-id] all-player-stats))))

(defn filter-players
  [stats player-ids]
  (-> stats
      (update-in [:round :players] #(select-keys % player-ids))
      (update-in [:all   :players] #(select-keys % player-ids))))
