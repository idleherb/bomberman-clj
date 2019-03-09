(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty 10x10 arena should be initialized"
    (let [arena (init-arena)]
      (is (not (nil? arena)))
      (is (vector? arena))
      (is (= 10 (count arena)))
      (is (every? #(= 10 (count %)) arena))
    )))
