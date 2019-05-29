(ns ^:figwheel-always bomberman-clj.client.core
  (:require [reagent.core :as r]
            [bomberman-clj.client.components.app :refer [app]]
            [bomberman-clj.client.ws]))

(enable-console-print!)

(r/render-component [app]
                    (.getElementById js/document "app"))
