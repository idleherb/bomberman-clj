(ns bomberman-clj.client.components.game.util
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn format-time
  [milliseconds]
  (let [tmp (int (/ milliseconds 1000))
        sec (mod tmp 60)
        tmp (int (/ tmp 60))
        min (mod tmp 60)
        tmp (int (/ tmp 60))
        hrs (mod tmp 60)]
    (str (gstring/format "%02d" hrs) ":"
         (gstring/format "%02d" min) ":"
         (gstring/format "%02d" sec))))

(defn- all-comparator
  [player-1 player-2]
  (cond
    (> (:wins player-2) (:wins player-1)) 1
    (< (:wins player-2) (:wins player-1)) -1
    (> (:deaths player-2) (:deaths player-1)) 1
    (< (:deaths player-2) (:deaths player-1)) -1
    (> (:kills player-2) (:kills player-1)) 1
    (< (:kills player-2) (:kills player-1)) -1
    (> (:suicides player-2) (:suicides player-1)) -1
    (< (:suicides player-2) (:suicides player-1)) 1
    :else 1))

(defn sorted-stats
  [stats]
  (-> stats
      (update-in [:all :players]
                 #(into
                   (sorted-map-by (fn [k1 k2]
                                    (all-comparator (get % k1)
                                                      (get % k2))))
                   %))))

(defn- accumulate-player-stats
  [round-stats all-stats]
  (cond-> all-stats
    true (update-in [:kills] + (:kills round-stats))
    true (update-in [:moves] + (:moves round-stats))
    (:won? round-stats) (update-in [:wins] inc)
    (:dead? round-stats) (update-in [:deaths] inc)
    (:suicide? round-stats) (update-in [:suicides] inc)))

(defn accumulate-stats
  "Add round stats to all stats"
  [stats]
  (-> stats
      (update-in [:all :players]
                 #(into {} (map (fn [[id all-stats]]
                            (let [round-stats (get-in stats [:round :players id])]
                              [id (accumulate-player-stats round-stats all-stats)])) %)))))