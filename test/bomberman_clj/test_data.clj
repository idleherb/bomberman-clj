(ns bomberman-clj.test-data)

(def config-bomb-count 1)
(def config-bomb-radius 2)

(defn make-timestamp [] 1552767537306)

(defn make-player
  ([player-id coords]
   {:bomb-count config-bomb-count
    :bomb-radius config-bomb-radius
    :coords coords
    :name (str "Mx " player-id)
    :player-id player-id})
  ([player-id]
   (make-player player-id nil)))

(defn- make-player-round-stats
  []
  {:kills 0
   :death? false
   :suicide? false
   :moves 0
   :items {:bomb 0
           :fire 0}})

(defn- make-player-all-stats
  [timestamp]
  {:joined-at timestamp
   :playing-time 0
   :kills 0
   :deaths 0
   :suicides 0
   :wins 0
   :moves 0
   :items {:bomb 0
           :fire 0}})

(defn make-cell-bomb-p1 [] {:bomb {:player-id :player-1, :timestamp (make-timestamp)}})
(defn make-cell-player [player-id] {:player-id player-id})
(defn make-cell-hard-block [] {:block {:type :hard}})
(defn make-cell-soft-block [] {:block {:type :soft}})
(defn make-cell-item-bomb [] {:item {:type :bomb}})

(defn- make-grid
  []
  (let [sbl (make-cell-soft-block)
        hbl (make-cell-hard-block)
        pl1 (make-cell-player :player-1)
        pl2 (make-cell-player :player-2)]
    {:v [pl1 pl2 sbl
         nil hbl nil
         nil nil nil]
     :width 3
     :height 3}))

(defn make-empty-game
  []
  {:in-progress? false
   :num-players 2
   :players {}
   :stats {:round {:started-at nil
                   :duration 0
                   :players {}}
           :all {:players {}}}
   :grid nil
   :width 3
   :height 3})

(defn make-game
  [timestamp]
  {:in-progress? true
   :num-players 2
   :width 3
   :height 3
   :stats {:round {:started-at timestamp
                   :duration 0
                   :players {:player-1 (make-player-round-stats)
                             :player-2 (make-player-round-stats)}}
           :all {:players {:player-1 (make-player-all-stats timestamp)
                           :player-2 (make-player-all-stats timestamp)}}}
   :grid (make-grid)
   :players {:player-1 (make-player :player-1 {:x 0, :y 0})
             :player-2 (make-player :player-2 {:x 1, :y 0})}})
