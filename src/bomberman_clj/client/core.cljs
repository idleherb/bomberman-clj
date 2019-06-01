(ns ^:figwheel-always bomberman-clj.client.core
  (:require [reagent.core :as r]
            [stylefy.core :as stylefy]
            [bomberman-clj.client.components.app :refer [app]]
            [bomberman-clj.client.ws]))

(enable-console-print!)

(stylefy/init)
(r/render-component [app]
                    (.getElementById js/document "app"))
