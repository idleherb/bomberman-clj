(ns bomberman-clj.client.state
  (:require [cljs.core.async :as async
                             :include-macros true]
            [reagent.core :as r]))

(defonce state (r/atom {:app {:actions-ch (async/chan)}
                        :game nil}))

(defn update-game!
  [game]
  (swap! state assoc :game game))
