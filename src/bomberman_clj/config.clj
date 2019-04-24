(ns bomberman-clj.config)

(def fps 10)

(def arena-width 17)
(def arena-height 15)

(def bomb-count 1)
(def bomb-radius 2)

(def spawn-max-tries 100)
(def chance-spawn-item 3/10)

(def bomb-timeout-ms 3000)

(def expiration-ms 1000)
(def gameover-expiration-ms 1500)

(def glyphs {:player-1 \@
             :player-2 \&
             :block {:hard \█
                     :soft \▒}
             :item {:bomb \X
                    :fire \#}})
