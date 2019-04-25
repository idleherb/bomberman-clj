(ns bomberman-clj.specs
  (:require [clojure.core.async :as async]
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

(s/def ::chan #(satisfies? clojure.core.async.impl.protocols/Channel %))
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

(s/def ::bombs map?)
(s/def ::players map?)
(s/def ::winner (s/and keyword? #(re-matches #"player-\d+" (name %))))
(s/def ::num-players (s/and int? #(<= 1 % max-players)))
(s/def ::in-progress? boolean?)
(s/def ::gameover (s/keys :req-un [::timestamp] opt-un [::winner]))
(s/def ::game (s/keys :req-un [::grid
                               ::in-progress?
                               ::num-players
                               ::players
                               ::width
                               ::height]
                      :opt-un [::gameover]))
