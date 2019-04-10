(ns bomberman-clj.test-data)

(defn make-timestamp [] 1552767537306)

(defn make-player-1 [] {:bomb-count 1, :glyph \@, :name "White Bomberman"})
(defn make-player-2 [] {:bomb-count 1, :glyph \&, :name "Pretty Bomber"})
(defn make-player-3 [] {:bomb-count 1, :glyph \^, :name "Black Bomberman"})

(defn make-cell-bomb-p1 [] {:bomb {:player-id :player-1, :timestamp (make-timestamp)}})
(defn make-cell-p1 [] {:player-1 (make-player-1)})
(defn make-cell-hard-block [] {:block {:type :hard}})
(defn make-cell-soft-block [] {:block {:type :soft}})
