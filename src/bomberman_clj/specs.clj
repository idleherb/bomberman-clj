(ns bomberman-clj.specs
  (:require [clojure.core.async.impl.protocols :as protocols]
            [clojure.spec.alpha :as s]))

(defn valid? [spec obj]
  (let [valid (s/valid? spec obj)]
    (when (not valid)
      (do
        (println)
        (println "E specs::valid? - vvvvvvvvvvvvvvvvvvvvvvvv")
        (println (str "E specs::valid? - obj:" obj))
        (println (s/explain-str spec obj))
        (println "E specs::valid? - ^^^^^^^^^^^^^^^^^^^^^^^^")
        (println)))
    valid))

(def max-grid-width 999)
(def max-grid-height 999)
(def max-players 999)

(s/def ::timestamp (s/and int? #(> % 0) #(= 13 (count (str %)))))
(s/def ::hit (s/keys :req-un [::timestamp]))
(s/def ::left (s/keys :req-un [::timestamp]))
(s/def ::type keyword?)
(s/def ::action keyword?)
(s/def ::player-id (s/and keyword? #(re-matches #"player-\d+" (name %))))

(s/def ::chan #(satisfies? protocols/Channel %))
(s/def ::event (s/keys :req-un [::type ::timestamp] ::opt-un [::action ::payload ::player-id]))

(s/def ::bomb-count (s/and int? #(>= % 0 )))

(s/def ::x int?)
(s/def ::y int?)
(s/def ::coords (s/nilable (s/keys :req-un [::x ::y])))

(s/def ::block (s/keys :req-un [::type] :opt-un [::hit]))

(s/def ::player (s/keys :req-un [::bomb-count
                                 ::bomb-radius
                                 ::coords
                                 ::name
                                 ::player-id]
                        :opt-un [::hit
                                 ::left]))

(s/def ::bomb (s/keys :req-un [::player-id ::timestamp]))
(s/def ::fire (s/keys :req-un [::timestamp]))

(s/def ::cell (s/nilable (s/keys :opt-un [::block
                                          ::bomb
                                          ::fire
                                          ::item
                                          ::player-id])))
(s/def ::v (s/coll-of ::cell
                      :min-count 1
                      :max-count (* max-grid-width max-grid-height)))
(s/def ::width (s/and int? #(>= max-grid-width % 1)))
(s/def ::height (s/and int? #(>= max-grid-height % 1)))
(s/def ::grid (s/nilable (s/keys :req-un [::v ::width ::height])))

(s/def ::bombs map?)  ; TODO: Remove
(s/def :stats.player/kills int?)
(s/def :stats.player.round/dead? boolean?)
(s/def :stats.player.round/suicide? boolean?)
(s/def :stats.player/moves int?)
(s/def :stats.player.items/bomb int?)
(s/def :stats.player.items/fire int?)
(s/def :stats.player/items (s/keys :req-un [:stats.player.items/bomb :stats.player.items/fire]))

(def stats-player-round-spec (s/keys :req-un [:stats.player/kills
                                              :stats.player.round/dead?
                                              :stats.player.round/suicide?
                                              :stats.player/moves
                                              :stats.player/items]))
(s/def ::player-round-stats stats-player-round-spec)
(s/def :stats.round/player-1 stats-player-round-spec)
(s/def :stats.round/player-2 stats-player-round-spec)
(s/def :stats.round/player-3 stats-player-round-spec)
(s/def :stats.round/player-4 stats-player-round-spec)
(s/def :stats.round/player-5 stats-player-round-spec)
(s/def :stats.round/player-6 stats-player-round-spec)
(s/def :stats.round/player-7 stats-player-round-spec)
(s/def :stats.round/player-8 stats-player-round-spec)
(s/def :stats.round/players (s/keys :opt-un [:stats.round/player-1
                                            :stats.round/player-2
                                            :stats.round/player-3
                                            :stats.round/player-4
                                            :stats.round/player-5
                                            :stats.round/player-6
                                            :stats.round/player-7
                                            :stats.round/player-8]))
(def stats-nilable-ts-spec (s/nilable (s/and int? #(> % 0) #(= 13 (count (str %))))))
(s/def :stats.round/started-at stats-nilable-ts-spec)
(s/def :stats.round/duration int?)
(s/def :stats/round (s/keys :req-un [:stats.round/started-at
                                     :stats.round/duration
                                     :stats.round/players]))

(s/def :stats.player.all/joined-at stats-nilable-ts-spec)
(s/def :stats.player.all/playing-time int?)
(s/def :stats.player.all/deaths int?)
(s/def :stats.player.all/suicides int?)
(s/def :stats.player.all/wins int?)
(def stats-player-all-spec (s/keys :req-un [:stats.player.all/joined-at
                                            :stats.player.all/playing-time
                                            :stats.player/kills
                                            :stats.player.all/deaths
                                            :stats.player.all/suicides
                                            :stats.player.all/wins
                                            :stats.player/moves
                                            :stats.player/items]))
(s/def ::player-all-stats stats-player-all-spec)
(s/def :stats.all/player-1 stats-player-all-spec)
(s/def :stats.all/player-2 stats-player-all-spec)
(s/def :stats.all/player-3 stats-player-all-spec)
(s/def :stats.all/player-4 stats-player-all-spec)
(s/def :stats.all/player-5 stats-player-all-spec)
(s/def :stats.all/player-6 stats-player-all-spec)
(s/def :stats.all/player-7 stats-player-all-spec)
(s/def :stats.all/player-8 stats-player-all-spec)
(s/def :stats.all/players (s/keys :opt-un [:stats.all/player-1
                                          :stats.all/player-2
                                          :stats.all/player-3
                                          :stats.all/player-4
                                          :stats.all/player-5
                                          :stats.all/player-6
                                          :stats.all/player-7
                                          :stats.all/player-8]))

(s/def :stats/all (s/keys :req-un [:stats.all/players]))
(s/def ::stats (s/keys :req-un [:stats/round :stats/all]))

(s/def ::players map?)
(s/def ::winner (s/and keyword? #(re-matches #"player-\d+" (name %))))
(s/def ::num-players (s/and int? #(<= 1 % max-players)))
(s/def ::in-progress? boolean?)
(s/def ::gameover (s/keys :req-un [::timestamp] :opt-un [::winner]))
(s/def ::game (s/keys :req-un [::grid
                               ::in-progress?
                               ::num-players
                               ::players
                               ::stats
                               ::width
                               ::height]
                      :opt-un [::gameover]))
