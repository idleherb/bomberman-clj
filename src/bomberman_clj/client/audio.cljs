(ns bomberman-clj.client.audio
  (:require [cljs-bach.synthesis :as b]
            [bomberman-clj.client.state :as s]))

(defn- play-mp3
  [url]
  (when-let [audio-context (get-in @s/state [:app :audio-context])]
    (let [mp3 (b/connect->
               (b/sample url)
               (b/gain 0.5)
               b/destination)]
      (b/run-with mp3 audio-context (b/current-time audio-context) 3.0))))

(defn explosion
  ([num]
   (let [num (if (some? num) num (+ 1 (rand-int 5)))
         url (str "/mp3/explosion_00" num ".mp3")]
     (play-mp3 url)))
  ([]
   (explosion nil)))

(defn- precache-samples!
  []
  (doall (for [num (range 1 6)]
    (explosion num)))
  (swap! s/state assoc-in [:app :precached?] true))

(defn- init-audio-context!
  []
  (let [precached? (get-in @s/state [:app :precached?])]
    (do
      (swap! s/state assoc-in [:app :audio-context] (b/audio-context))
      (when-not precached? (precache-samples!)))))

(defn mute!
  []
  (when-let [audio-context (get-in @s/state [:app :audio-context])]
    (do
      (.close audio-context)
      (swap! s/state assoc-in [:app :audio-context] nil)
      (swap! s/state assoc-in [:app :mute?] true))))

(defn unmute!
  []
  (when-not (get-in @s/state [:app :audio-context])
    (do
      (init-audio-context!)
      (swap! s/state assoc-in [:app :mute?] false))))
