(ns bomberman-clj.game-loop
  (:require [clojure.core.async :as async]
            [bomberman-clj.game :as game]
            [bomberman-clj.util :as util]))

(defn- key-set
  [map]
  (into #{} (keys map)))

(defn- key-diff
  [sub-map super-map]
  (into #{} (remove (key-set sub-map) (key-set super-map))))

(defn- on-join!
  [event game ch]
  (println "D game-loop::on-join! - client requests join...")
  (let [{:keys [payload timestamp]} event
        {:keys [num-players players]} game
        player payload
        game (game/join game player timestamp)
        new-players (key-diff players (:players game))
        player-id (condp = (count new-players)
          0 nil
          1 (first new-players)
          (throw (Exception. (str "E game-loop::on-join! - too many new players:" players))))]
    (if (some? player-id)
      (do
        (println "I game-loop::on-join! -" player-id "joined:" player)
        (if (= num-players (count (:players game)))
          (do
            (println "I game-loop::on-join! - all player slots filled, starting new round...")
            (game/next-round game timestamp))
          game))
      (do
        (async/go (async/>! ch {:broadcast false, :type :error, :payload "no free player slots"}))
        game))))

(defn- on-action
  [event game ch]
  (let [{:keys [payload timestamp]} event
        {:keys [action player-id]} payload]
    (condp = action
      :detonate-bombs (game/remote-detonate-bombs game player-id timestamp)
      :move (game/move game player-id (:direction payload) timestamp)
      :plant-bomb (game/plant-bomb game player-id timestamp)
      (do
        (println "W game-loop::on-action - unknown player action:" action)
        game))))

(defn- on-leave!
  [event game ch]
  ; (println "D game-loop::on-leave! - client requests leave...")
  (let [{:keys [payload timestamp]} event]
    (game/leave game (:player-id payload) timestamp)))

(defn- on-refresh!
  [event game ch]
  ; (println "D game-loop::on-refresh! - client requests update...")
  (let [timestamp (:timestamp event)]
    (if (:in-progress? game)
      (let [game (game/eval game timestamp)]
        (if (and (game/game-over? game) (util/gameover-expired? (:gameover game) timestamp))
          (let [game (game/next-round game (:timestamp event))]
            (do
              (async/go (async/>! ch {:broadcast true
                                      :type :refresh
                                      :payload game
                                      :timestamp timestamp}))
              game))
          (do
            (async/go (async/>! ch {:broadcast true
                                    :type :refresh
                                    :payload game
                                    :timestamp timestamp}))
            game)))
      (do
        (async/go (async/>! ch {:broadcast true
                                :type :refresh
                                :payload game
                                :timestamp timestamp}))
        game))))

(defn- on-dev-restart
  [event game ch]
  ; (println "D game-loop::on-dev-restart - event:" event)
  (game/next-round game (:timestamp event)))

(defn- abort-game!
  [ch]
  ; (println "W game-loop::abort-game! - game aborted...")
  (async/go (async/>! ch {:broadcast true, :type :error, :payload "game aborted"})))

(defn game-loop
  [ch-in ch-out num-players width height]
  (async/go-loop [game (game/init num-players width height)]
    (if-let [event (async/<! ch-in)]
      (let [game (condp = (:type event)
                   :join        (on-join!       event game ch-out)
                   :action      (on-action      event game ch-out)
                   :leave       (on-leave!      event game ch-out)
                   :refresh     (on-refresh!    event game ch-out)
                   :dev-restart (on-dev-restart event game ch-out)
                   :dev-exit    nil
                   game)]
        (when (some? game) (recur game)))
      (abort-game! ch-out))))
