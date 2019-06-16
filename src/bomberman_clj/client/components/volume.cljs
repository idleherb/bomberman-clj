(ns bomberman-clj.client.components.volume
  (:require [bomberman-clj.client.audio :as audio]
            [bomberman-clj.client.state :as s]))

(defn- swap-mute?!
  []
  (let [mute? (get-in @s/state [:app :mute?])]
    (if mute?
      (audio/unmute!)
      (audio/mute!))))

(defn volume
  [mute?]
  [:button {:class ["emoji volume" (if mute? "volume--muted" "volume--unmuted")]
            :on-click swap-mute?!}])
