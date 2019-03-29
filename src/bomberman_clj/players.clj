(ns bomberman-clj.players
  (:require [bomberman-clj.config :as config]))

(defn init-player
  "Add default properties to the given player"
  [player]
  (merge player {:bomb-count config/bomb-count}))

(defn has-bombs?
  [player]
  (> (:bomb-count player) 0))

(defn dec-bombs
  [player]
  (if (has-bombs? player)
    (assoc player :bomb-count (dec (:bomb-count player)))
    player))

(defn inc-bombs
  [player]
  (assoc player :bomb-count (inc (:bomb-count player))))
