(ns bomberman-clj.config)

(def fps 10)

(def arena-width 17)
(def arena-height 15)

(def bomb-count 1)
(def bomb-radius 2)

(def player-spawn-max-num-tries 100)

(def item-types      [:bomb :fire])
(def rare-item-types [:bomb-kick :remote-control])

(def chance-spawn-item 3/10)
(def chance-spawn-rare-item 1/20)

(def bomb-kick-speed-ms 50)
(def bomb-timeout-ms 3000)

(def expiration-ms 1000)
(def gameover-expiration-ms 1500)
