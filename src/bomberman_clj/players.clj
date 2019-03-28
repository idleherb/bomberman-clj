(ns bomberman-clj.players)

(defn has-bombs?
  [player]
  (> (:bomb-count player) 0))

(defn dec-bombs
  [player]
  (if (has-bombs? player)
    (assoc player :bomb-count (dec (:bomb-count player)))
    player))
