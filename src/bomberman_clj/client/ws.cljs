(ns bomberman-clj.client.ws
  (:require [cljs.core.async :as async
                             :include-macros true]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [bomberman-clj.client.state :as s]))

(def ws-protocol (clojure.string/replace js/window.location.protocol "http" "ws"))
(def ws-url (str ws-protocol "//0.0.0.0:8080"))

(defn- forward-actions
  [ws-ch]
  (async/pipe (get-in @s/state [:app :actions-ch]) (:sink ws-ch)))

(defn- ws-event-loop
  [ws-ch]
  (async/go-loop []
    (when-let [{:keys [type, payload], :as event} (async/<! (:source ws-ch))]
      (condp = type
        :error (println "E ws::ws-event-loop - error:" payload)
        :message (println "D ws::ws-event-loop - message:" payload)
        :refresh (s/update-game! payload)
        (println "E ws::ws-event-loop - invalid event:" event))
      (recur))))

(swap! s/state assoc-in [:app :actions-ch] (async/chan))

(async/go
  (when-let [ws-ch (async/<! (ws/connect ws-url {:format fmt/edn}))]
    (println "D ws - connected to" ws-url)
    (forward-actions ws-ch)
    (ws-event-loop ws-ch)))
