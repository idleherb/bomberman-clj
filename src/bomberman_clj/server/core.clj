(ns bomberman-clj.server.core
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :as route]
            [immutant.web :as web]
            [immutant.web.middleware :as web-middleware]
            [ring.util.response :refer [redirect]]
            [bomberman-clj.server.ws :as ws]))

(defroutes routes
  (GET "/" {c :context} (redirect (str c "/index.html")))
  (route/resources "/"))

(defn run
  [host port]
  (ws/loop-broadcast-lobby 1)
  (web/run (-> routes
               (web-middleware/wrap-session {:timeout 20})
               (web-middleware/wrap-websocket (ws/ws-callbacks)))
           {"host" host, "port" port}))
