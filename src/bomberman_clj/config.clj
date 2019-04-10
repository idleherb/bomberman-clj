(ns bomberman-clj.config)

(def bomb-count 3)
(def bomb-radius 3)

(def bomb-timeout-ms 3000)

(def bomb-expiration-ms 1000)
(def fire-expiration-ms 1000)
(def player-expiration-ms 1000)

(def spawn-max-tries 100)

(def fps 10)

(def arena-width 17)
(def arena-height 15)

(def glyphs {:player-1 \@
             :player-2 \&
             :wall {:solid \█}})