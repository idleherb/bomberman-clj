(ns bomberman-clj.server.ws
(:require [bomberman-clj.specs :as specs]
          [clojure.core.async :as async]
          [cheshire.core :refer [generate-string parse-string]]
          [immutant.web.async :as ws-async]))

(def ws-chans (atom {}))  ; ch -> player-id

(defn- get-chan-player-id
  [ws-ch]
  (get @ws-chans ws-ch))

(defn- assoc-chan-player-id!
  [ws-ch player-id]
  (swap! ws-chans assoc ws-ch player-id))

(defn- dissoc-chan!
  [ws-ch]
  (swap! ws-chans dissoc ws-ch))

(defn- now
  []
  (System/currentTimeMillis))

(defn- get-free-player-id
  [num-players player-ids]
  (loop [i 1]
    (when (<= i num-players)
      (let [player-id (keyword (str "player-" i))]
        (if (not (contains? player-ids player-id))
          player-id
          (recur (inc i)))))))

(defn- add-player!
  [ws-ch max-players]
  (dosync
    (let [ws-chans @ws-chans
          num-players (count (filter (complement nil?) (vals ws-chans)))]
      (cond
        (not (nil? (get ws-chans ws-ch))) {:error "player already joined"}
        (= num-players max-players) {:error "no free player slots"}
        :else (if-let [player-id (get-free-player-id max-players (into #{} (vals ws-chans)))]
          (do (assoc-chan-player-id! ws-ch player-id)
            player-id)
            {:error "failed to add player"})))))

(defn- add-spectator!
  [ws-ch]
  (assoc-chan-player-id! ws-ch nil))

(defn- ws-on-open!
  [ch ws-ch]
  (add-spectator! ws-ch))

(defn broadcast
  [event]
  (doseq [ch (keys @ws-chans)] (ws-async/send! ch (generate-string event))))

(defn- send-message!
  [ws-ch message]
  (ws-async/send! ws-ch (generate-string {:type :message, :payload message})))

(defn- send-error!
  [ws-ch error]
  (ws-async/send! ws-ch (generate-string {:type :error, :payload error})))

(defn- on-join!!
  [event ch ws-ch num-players]
  (let [player (:payload event)]
    (let [{error :error, :as player-id} (add-player! ws-ch num-players)
          player (assoc player :player-id player-id)]
      (if (nil? error)
        (do
          (async/>!! ch {:type :join
                         :payload player
                         :timestamp (now)})
          (println (str "D ws::on-join!! - " player-id " joined game")))
        (do
          (println "W ws::ws-on-join!! - error:" error)
          (send-error! ws-ch error))))))

(defn- on-action!
  [event ch ws-ch]
  (when-let [player-id (get-chan-player-id ws-ch)]
    (async/go (async/>! ch {:type :action
                            :payload (assoc (:payload event) :player-id player-id)
                            :timestamp (now)}))))

(defn- on-leave!!
  [event ch ws-ch]
  (println "D ws::on-leave!! - event:" event)
  (if-let [player-id (get-chan-player-id ws-ch)]
    (do
      (async/>!! ch {:type :leave
                     :payload {:player-id player-id}
                     :timestamp (now)})
      (broadcast {:type :message
                  :payload (str player-id " left the game")})
      (add-spectator! ws-ch))
    (let [error "spectators can't leave the game"]
      (println "W ws::on-leave!! -" error)
      (send-error! ws-ch {:error error}))))

(defn- parse-event
  [message]
  (let [{:keys [type payload], :as event} (parse-string message true)
        type (keyword type)
        payload (if (= type :action)
          (assoc payload :action (keyword (:action payload)))
          payload)
        payload (if (contains? payload :direction)
          (assoc payload :direction (keyword (:direction payload)))
          payload)]
    (assoc event :type type
                 :payload payload)))

(defn- ws-on-message!
  [ch num-players ws-ch message]
  (let [{:keys [type payload timestamp], :as event} (parse-event message)
        player-id (get-chan-player-id ws-ch)]
    (condp = type
      :join   (on-join!!  event ch ws-ch num-players)
      :action (on-action! event ch ws-ch)
      :leave  (on-leave!! event ch ws-ch)
      (println "W ws::ws-on-message! - invalid event:" event))))

(defn- ws-on-close!
  [ch ws-ch status-map]
  (when-let [player-id (get-chan-player-id ws-ch)]
    (dissoc-chan! ws-ch)
    (async/go (async/>! ch {:type :leave
                            :payload {:player-id player-id}
                            :timestamp (now)}))
    (println "I ws::ws-on-close! -" player-id "disconnected...")))

(defn broadcast
  [event]
  (doseq [ch (keys @ws-chans)] (ws-async/send! ch (generate-string event))))

(defn ws-callbacks
  [ch-game-in num-players]
  {:pre [(specs/valid? ::specs/chan ch-game-in)]}
  {:on-open (partial ws-on-open! ch-game-in)
   :on-close (partial ws-on-close! ch-game-in)
   :on-message (partial ws-on-message! ch-game-in num-players)})
