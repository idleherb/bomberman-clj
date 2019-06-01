(ns bomberman-clj.client.components.cell
  (:require [bomberman-clj.client.audio :as audio]
            [bomberman-clj.client.state :as s]))

(defn- ensure-explosion-audio! [bomb cell-idx]
  (when (and (some? (:detonated bomb))
             (not (:audio-played? bomb)))
    (do
      (audio/explosion)
      (swap! s/state assoc-in [:game :grid :v cell-idx :bomb :audio-played?] true))))

(defn cell [cell-state cell-idx players]
  (let [{:keys [block bomb fire item player-id]} cell-state]
    (ensure-explosion-audio! bomb cell-idx)
    (let [classes (flatten ["cell"
                            (when (nil? cell-state) "empty")
                            (when-let [{:keys [type hit]} block]
                              ["block"
                               (name type)
                               (when hit "hit")])
                            (when (and bomb (not (:detonated bomb)))
                              (if (get-in players [(:player-id bomb) :remote-control?])
                                "bomb-remote-controlled"
                                "bomb"))
                            (when fire "fire")
                            (when-let [{:keys [type hit]} item]
                              ["item"
                               (str "item-" (name type))
                               (when hit "hit")])
                            (when player-id [player-id
                                             (when (:hit (get players player-id)) "hit")])])]
      [:div {:class classes}])))
