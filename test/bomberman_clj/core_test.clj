(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena should be initialized"
    (let [w 17
          h 15
          arena (init-arena w h)]
      (is (not (nil? arena)))
      (is (vector? arena))
      (is (= w (count arena)))
      (is (every? #(= h (count %)) arena))
    )))
