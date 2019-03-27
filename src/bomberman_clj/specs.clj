(ns bomberman-clj.specs
  (:require [clojure.spec.alpha :as s]))

(def max-grid-width 20)
(def max-grid-height 20)

(s/def ::glyph char?)

(s/def ::x int?)
(s/def ::y int?)
(s/def ::coords (s/and (s/coll-of int? :kind vector) #(= 2 (count %))))  ; TODO: {:x, :y}

(s/def ::timestamp (s/and int? #(> % 0) #(= 13 (count (str %)))))
(s/def ::hit (s/keys :req-un [::timestamp]))
(s/def ::player-1 (s/keys :req-un [::glyph] ::opt-un [::coords ::hit]))
(s/def ::player-2 (s/keys :req-un [::glyph] ::opt-un [::coords ::hit]))

(s/def ::bomb (s/keys :req-un [::timestamp]))
(s/def ::fire (s/keys :req-un [::timestamp]))

(s/def ::cell (s/keys :opt-un [::bomb ::fire ::player-1 ::player-2]))
(s/def ::v (s/coll-of (s/nilable ::cell)
                      :min-count 1
                      :max-count (* max-grid-width max-grid-height)))
(s/def ::width (s/and int? #(>= max-grid-width % 1)))
(s/def ::height (s/and int? #(>= max-grid-height % 1)))
(s/def ::grid (s/keys :req-un [::v ::width ::height]))

(s/def ::bombs map?)

(s/def ::player-1 (s/and (s/coll-of int? :kind vector) #(= 2 (count %))))
(s/def ::player-2 (s/and (s/coll-of int? :kind vector) #(= 2 (count %))))
(s/def ::players (s/keys :req-un [::player-1] :opt-un [::player-2]))
(s/def ::arena (s/keys :req-un [::bombs ::grid ::players])
