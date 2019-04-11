(ns bomberman-clj.cells
  (:require [bomberman-clj.specs :as specs]))

(defn cell-bomb
  "Return the bomb in the given cell if any"
  [cell]
  ; {:pre [(specs/valid? ::specs/cell cell)]}
  (:bomb cell))

(defn cell-player-id
  "Checks if the given cell contains a player and returns its mapping, else nil"
  [cell]
  ; {:pre [(specs/valid? ::specs/cell cell)]}
  (first ; player-id (or nil)
    (first ; first (or nil) [player-id, player]
      (filter (fn [[k _]] (re-matches #"player-\d+" (name k))) cell))))

(defn cell-player
  "Return the player in the given cell if any"
  ([cell]
   ; {:pre [(specs/valid? ::specs/cell cell)]}
   (cell-player cell (cell-player-id cell)))
  ([cell player-id]
   ; {:pre [(specs/valid? ::specs/cell cell)]}
   (when (not (nil? player-id)) (player-id cell))))
