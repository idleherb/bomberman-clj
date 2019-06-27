(ns bomberman-clj.domain.game-loop.core
  (:require [clojure.core.async :as a]
            [bomberman-clj.domain.game.core :as g]))

(defn- key-set
  [map]
  (into #{} (keys map)))

(defn- key-diff
  [sub-map super-map]
  (into #{} (remove (key-set sub-map) (key-set super-map))))

(defn- put-error!
  [ch msg]
  (a/go (a/>! ch {:type    :error
                  :payload msg})))

(defn- on-join!
  [event game ch]
  (let [{:keys [payload timestamp]} event
        {:keys [num-players players]} game
        player payload
        game (g/join game player timestamp)
        new-players (key-diff players (:players game))
        player-id (condp = (count new-players)
          0 nil
          1 (first new-players)
          (throw (Exception. (str "E d.gl.core::on-join! - too many new players:" players))))]
    (if (some? player-id)
      (do
        (println "D d.gl.core::on-join! -" player-id "joined:" player)
        (if (= num-players (count (:players game)))
          (do
            (println "D d.gl.core::on-join! - all player slots filled, starting new round...")
            (g/next-round game timestamp))
          game))
      (do
        (put-error! ch "no free player slots")
        game))))

(defn- on-leave!
  [event game ch]
  (let [{:keys [payload timestamp]} event]
    (g/leave game (:player-id payload) timestamp)))

(defn- on-action
  [event game ch]
  (let [{:keys [payload timestamp]} event
        {:keys [action player-id]}  payload]
    (condp = action
      :detonate-bombs (g/remote-detonate-bombs game player-id timestamp)
      :move           (g/move game player-id (:direction payload) timestamp)
      :plant-bomb     (g/plant-bomb game player-id timestamp)
      (do
        (println "W d.gl.core::on-action - unknown player action:" action)
        game))))

(defn- put-game-and-return!
  [ch game timestamp]
  (a/go (a/>! ch {:type      :refresh
                  :payload   game
                  :timestamp timestamp}))
  game)

(defn- on-refresh!
  [event game ch]
  (let [ts (:timestamp event)]
    (if (:in-progress? game)
      (let [eval-game (g/eval game ts)]
        (if (g/gameover-expired? eval-game ts)
          (put-game-and-return! ch (g/next-round eval-game ts) ts)
          (put-game-and-return! ch eval-game ts)))
      (put-game-and-return! ch game ts))))

(defn game-loop
  [ch-in ch-out num-players width height]
  (a/go-loop [game (g/init num-players width height)]
    (if-let [event (a/<! ch-in)]
      (let [game (condp = (:type event)
                   :join    (on-join!    event game ch-out)
                   :leave   (on-leave!   event game ch-out)
                   :action  (on-action   event game ch-out)
                   :refresh (on-refresh! event game ch-out)
                   :close   nil
                   game)]
        (if (some? game)
          (recur game)
          (println "D d.gl.core::game-loop - close")))
      (put-error! ch-out "game aborted"))))
