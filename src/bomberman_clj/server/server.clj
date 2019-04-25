(ns bomberman-clj.server.server
  (:require [bomberman-clj.server.ws :as ws]
            [clojure.core.async :as async]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [immutant.web :as web]
            [immutant.web.middleware :as web-middleware]
            [ring.util.response :refer [redirect]]))

(defroutes routes
  (GET "/" {c :context} (redirect (str c "/index.html")))
  (route/resources "/"))

(defn- game-update-listener!
  [ch-game-out]
  (async/go-loop []
    (if-let [event (async/<! ch-game-out)]
      (do
        (ws/broadcast event)
        (recur))
      (println "D server::game-update-listener! - aborted..."))))

(defn run
  [ch-game-in ch-game-out num-players
   ; host port & {:as args}
   ]
  (web/run
    (-> routes
        (web-middleware/wrap-session {:timeout 20})
        (web-middleware/wrap-websocket (ws/ws-callbacks ch-game-in num-players)))
    ;(merge {"host" host, "port" port} args)
  )
  (game-update-listener! ch-game-out))
