(ns bomberman-clj.test-data)

(defn make-timestamp [] 1552767537306)

(defn make-player-1 [] {:bomb-count 1
                        :bomb-radius 3
                        :coords nil
                        :name "White Bomberman"
                        :player-id :player-1})
(defn make-player-2 [] {:bomb-count 1
                        :bomb-radius 3
                        :coords nil
                        :name "Pretty Bomber"
                        :player-id :player-2})
(defn make-player-3 [] {:bomb-count 1
                        :bomb-radius 3
                        :coords nil
                        :name "Black Bomberman"
                        :player-id :player-3})

(defn make-empty-game [] {:in-progress? false
                          :num-players 2
                          :players {}
                          :stats {:round {:started-at nil
                                          :duration 0
                                          :players {}}
                                  :all {:players {}}}
                          :grid nil
                          :width 17
                          :height 15})

(defn make-player-round-stats [] {:kills 0
                                  :death? false
                                  :suicide? false
                                  :moves 0
                                  :items {:bomb 0
                                          :fire 0}})


(defn make-player-all-stats [ts] {:joined-at ts
                                  :playing-time 0
                                  :kills 0
                                  :deaths 0
                                  :suicides 0
                                  :wins 0
                                  :moves 0
                                  :items {:bomb 0
                                          :fire 0}})

(defn make-cell-bomb-p1 [] {:bomb {:player-id :player-1, :timestamp (make-timestamp)}})
(defn make-cell-p1 [] {:player-id :player-1})
(defn make-cell-p2 [] {:player-id :player-2})
(defn make-cell-hard-block [] {:block {:type :hard}})
(defn make-cell-soft-block [] {:block {:type :soft}})
(defn make-cell-item-bomb [] {:item {:type :bomb}})
