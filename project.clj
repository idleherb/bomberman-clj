(defproject bomberman-clj "0.1.0"
  :description "A basic Bomberman game written in Clojure."
  :url "https://github.com/idleherb/bomberman-clj"
  :license {:name "The Unlicense"
            :url "https://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.immutant/web "2.1.10"]
                 [cheshire "5.8.1"]
                 [clojure-lanterna "0.9.7"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.1"]]
  :managed-dependencies [[org.clojure/core.rrb-vector "0.0.13"]
                         [org.flatland/ordered "1.5.7"]]
  :main ^:skip-aot bomberman-clj.core
  :min-lein-version "2.0.0"
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.9.7" :exclusions [org.clojure/clojure]]]
                   :plugins [[lein-cloverage "1.1.1"]
                             [lein-midje "3.2.1"]]}})
