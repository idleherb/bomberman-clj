(ns bomberman-clj.server.ws
  (:require [clojure.core.async :as a]
            [clojure.edn :as edn]
            [digest]
            [immutant.web.async :as wsa]
            [bomberman-clj.config :as c]
            [bomberman-clj.domain.game-loop.core :as gl]
            [bomberman-clj.domain.game-loop.frame-rate :as fps]))

(def games (atom {})) ; game-id -> {:ch-in :num-players ...}
(def players (atom {})) ; ch -> {:player-id ...
                        ;        :game-id ...
                        ;        :lobby? boolean?
                        ;        :role (:spectator|:player)}

(defn- now
  []
  (System/currentTimeMillis))

(defn broadcast-game
  [game-id event]
  (let [player-chans (->> @players
                          (filter #(= game-id (:game-id (second %))))
                          (map first))]
    (doseq [ch player-chans] (wsa/send! ch (pr-str event)))))

(defn- get-lobby
  []
  (into []
    (map #(select-keys (second %) [:game-id
                                   :name
                                   :cur-num-players
                                   :num-players
                                   :height
                                   :width
                                   :admin-ws-ch])
         @games)))

(defn loop-broadcast-lobby
  [fps]
  (a/go-loop []
    (a/<! (a/timeout (/ 1000 fps)))
    (when-let [guest-chans (not-empty
                            (->> @players
                                 (filter #(:lobby? (second %)))
                                 (map first)))]
        (doseq [ws-ch guest-chans]
          (let [lobby (into [] (map #(-> %
                                         (assoc :admin? (= ws-ch (:admin-ws-ch %)))
                                         (dissoc :admin-ws-ch))
                                    (get-lobby)))
                event {:type :refresh-lobby
                       :payload lobby
                       :timestamp (now)}]
            (wsa/send! ws-ch (pr-str event)))))
    (recur)))

(defn- send-error!
  [ws-ch error]
  (wsa/send! ws-ch (pr-str {:type :error, :payload error})))

(defn- ws-on-connect!
  [ws-ch]
  (swap! players assoc ws-ch {:lobby? true}))

(defn- get-player
  [ws-ch]
  (get @players ws-ch))

(defn- get-game
  [game-id]
  (get @games game-id))

(defn- into-lobby!
  [ws-ch]
  (when-let [player (get-player ws-ch)]
    (let [player (assoc player :game-id nil
                               :player-id nil
                               :role nil
                               :lobby? true)]
      (swap! players assoc ws-ch player))))

(defn- destroy-game!
  [game-id ch-in]
  (a/>!! ch-in {:type :close
                    :timestamp (now)})
  (swap! games dissoc game-id)
  (doseq [[ws-ch player] @players]
    (when (= game-id (:game-id player)) (into-lobby! ws-ch))))

(defn- on-leave!!
  [event ws-ch]
  (when-let [{:keys [role game-id player-id]} (get-player ws-ch)]
    (when (= role :player)
      (if-let [{ch-in :ch-in} (get-game game-id)]
        (do
          (a/>!! ch-in {:type :leave
                            :payload {:player-id player-id}
                            :timestamp (now)})
          (dosync
           (into-lobby! ws-ch)
           (swap! games update-in [game-id :cur-num-players] dec)))
        (println "E server.ws::on-leave!! - unknown game:" game-id)))))

(defn- on-close!!
  [event ws-ch]
  (let [game-id (:payload event)
        {role :role} (get-player ws-ch)]
    (if-let [{:keys [admin-ws-ch ch-in]} (get-game game-id)]
      (if (= ws-ch admin-ws-ch)
        (do
          (when (= role :player) (on-leave!! nil ws-ch))
          (destroy-game! game-id ch-in))
        (send-error! ws-ch "only the game admin can close the game"))
      (println "E server.ws::on-close!! - unknown game:" game-id))))

(defn- get-admin-game-id
  [ws-ch]
  (first
   (for [[id game] @games
         :when (= ws-ch (:admin-ws-ch game))]
     id)))

(defn- ws-on-disconnect!!
  [ws-ch status-map]
  (on-leave!! nil ws-ch)
  (when-let [game-id (get-admin-game-id ws-ch)]
    (on-close!! {:payload game-id} ws-ch))
  (swap! players dissoc ws-ch))

(defn- game-update-listener!
  [game-id ch-out]
  (a/go-loop []
    (if-let [event (a/<! ch-out)]
      (do
        (broadcast-game game-id event)
        (recur))
      (println "D server.ws::game-update-listener! - aborted"))))

(defn- make-game-id
  [name]
  (subs (str (digest/sha-256 name)) 0 10))

(defn- open-game!
  [ws-ch game-id name num-players width height]
  (let [ch-in (a/chan)
        ch-out (a/chan)
        ch-game (gl/game-loop ch-in ch-out num-players width height)
        ch-fps (fps/set ch-in c/fps)]
    (a/go
      (if-let [event (a/<! ch-game)]
        (if (= (:type event) :exit)
          (do
            (println "D server.ws::open-game! - client requested game exit...")
            (fps/unset ch-fps)
            (a/close! ch-game)
            (a/close! ch-out)
            (a/close! ch-in)
            (println "D server.ws::open-game! - exit."))
          (println "E server.ws::open-game! - invalid event:" event))
        (println "W server.ws::open-game! - aborted.")))
    {:ch-in ch-in
     :ch-out ch-out
     :ch-game ch-game
     :ch-fps ch-fps
     :admin-ws-ch ws-ch
     :game-id game-id
     :name name
     :num-players num-players
     :cur-num-players 0
     :width width
     :height height}))

(defn- on-open!
  [event ws-ch]
  (let [{:keys [name height width num-players]} (:payload event)
        game-id (make-game-id name)]
    (if (get @games game-id)
      (send-error! ws-ch (str "game name " name " already taken"))
      (let [{ch-out :ch-out, :as game} (open-game! ws-ch game-id name num-players width height)]
        (game-update-listener! game-id ch-out)
        (swap! games assoc game-id game)))))

(defn- get-free-player-id
  [player-ids num-players]
  (when (< (count player-ids) num-players)
    (loop [i 1]
      (let [player-id (keyword (str "player-" i))]
        (if-not (some #{player-id} player-ids)
          player-id
          (recur (inc i)))))))

(defn- on-join!!
  [event ws-ch]
  (let [{:keys [game-id], :as player} (get-player ws-ch)]
    (if game-id
      (send-error! ws-ch (str "already a member of game " game-id ", can't join another game"))
      (let [{:keys [game-id player-name]} (:payload event)
            {:keys [ch-in num-players]} (get-game game-id)
            player-ids (into []
                          (->> @players
                               (filter #(and (= game-id (:game-id (second %)))
                                             (= :player (:role (second %)))))
                               (map #(:player-id (second %)))))]
        (if-let [player-id (get-free-player-id player-ids num-players)]
          (do
            (a/>!! ch-in {:type :join
                              :payload {:player-id player-id
                                        :name player-name}
                              :timestamp (now)})
            (dosync
             (swap! games update-in [game-id :cur-num-players] inc)
             (swap! players assoc ws-ch
                    (assoc player :game-id game-id
                                  :player-id player-id
                                  :lobby? false
                                  :role :player)))
            (println (str "D ws::on-join!! - " player-id " joined game " game-id)))
          {:error "no free player slots"})))))

(defn- on-action!
  [event ws-ch]
  (let [{:keys [game-id player-id]} (get-player ws-ch)]
    (when player-id
      (let [{:keys [ch-in]} (get-game game-id)]
        (a/go (a/>! ch-in {:type :action
                                   :payload (assoc (:payload event) :player-id player-id)
                                   :timestamp (now)}))))))

(defn- ws-on-message!
  [ws-ch message]
  (let [{type :type, :as event} (edn/read-string message)]
    (condp = type
      :open   (on-open! event ws-ch)
      :close  (on-close!! event ws-ch)
      :join   (on-join!! event ws-ch)
      :leave  (on-leave!! event ws-ch)
      :action (on-action! event ws-ch)
      (println "W server.ws::ws-on-message! - invalid event:" event))))

(defn ws-callbacks
  []
  {:on-open ws-on-connect!
   :on-close ws-on-disconnect!!
   :on-message ws-on-message!})
