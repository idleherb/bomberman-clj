(ns bomberman-clj.client.state
  (:require [cljs.core.async :as async
                             :include-macros true]
            [reagent.core :as r]))

(defonce state (r/atom {:app {:actions-ch (async/chan)
                              :mute? true
                              :precached? false
                              :stale-style? true
                              :game-dialog {:open? false
                                            :name ""
                                            :width 17
                                            :height 15
                                            :num-players 2}
                              :player-dialog {:open? false
                                              :game-id nil
                                              :name ""}}
                        :game nil
                        :lobby []}))

(defn- preprocess-cells
  [width height v prev-v]
  (into []
    (for [y (range height)
          x (range width)]
      (let [i (+ (* y width) x)
            cell (nth v i)
            bomb (:bomb cell)
            prev-audio-played? (get-in prev-v [i :bomb :audio-played?])]
        (cond
          (and prev-audio-played?
               (some? bomb)) (assoc-in cell [:bomb :audio-played?] prev-audio-played?)
          :else cell)))))

(defn- preprocess-game
  [game prev-game]
  (let [v (get-in game [:grid :v])
        prev-v (get-in prev-game [:grid :v])]
    (if (or (= v prev-v) (nil? prev-v))
      game
      (let [{:keys [width height]} game]
        (assoc-in game
                  [:grid :v]
                  (preprocess-cells width height v prev-v))))))

(defn update-game!
  [game]
  (let [{prev-game :game} @state
        game (preprocess-game game prev-game)]
    (swap! state assoc :game game
                       :lobby nil)))

(defn update-lobby!
  [lobby]
  (swap! state assoc :lobby lobby
                     :game nil))
