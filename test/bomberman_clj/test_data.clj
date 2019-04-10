(ns bomberman-clj.test-data)

(defn make-timestamp [] 1552767537306)

(defn make-player-1 [] {:glyph \P, :bomb-count 1})
(defn make-player-2 [] {:glyph \Q, :bomb-count 1})
(defn make-player-3 [] {:glyph \R, :bomb-count 1})

(defn make-cell-bomb-p1 [] {:bomb {:player-id :player-1, :timestamp (make-timestamp)}})
(defn make-cell-p1 [] {:player-1 (make-player-1)})
(defn make-cell-wall [] {:wall :solid})
