(ns bomberman-clj.client.state
  (:require [reagent.core :as r]))

(defonce state (r/atom {:app {}
                        :game nil}))

(defn update-game!
  [game]
  (swap! state assoc :game game))
