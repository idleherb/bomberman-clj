(ns bomberman-clj.test-data)

(defn make-timestamp [] 1552767537306)

(defn make-player-1 [] {:bomb-count 1
                        :bomb-radius 3
                        :glyph \@
                        :name "White Bomberman"
                        :player-id :player-1})
(defn make-player-2 [] {:bomb-count 1
                        :bomb-radius 3
                        :glyph \&
                        :name "Pretty Bomber"
                        :player-id :player-2})
(defn make-player-3 [] {:bomb-count 1
                        :bomb-radius 3
                        :glyph \^
                        :name "Black Bomberman"
                        :player-id :player-3})

(defn make-cell-bomb-p1 [] {:bomb {:player-id :player-1, :timestamp (make-timestamp)}})
(defn make-cell-p1 [] {:player (make-player-1)})
(defn make-cell-p2 [] {:player (make-player-2)})
(defn make-cell-hard-block [] {:block {:type :hard}})
(defn make-cell-soft-block [] {:block {:type :soft}})
(defn make-cell-item-bomb [] {:item {:type :bomb}})
