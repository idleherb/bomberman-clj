(defproject bomberman-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "The Unlicense"
            :url "https://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clojure-lanterna "0.9.7"]]
  :main ^:skip-aot bomberman-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
