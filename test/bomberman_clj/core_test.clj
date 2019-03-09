(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena should be initialized"
    (let [width 17
          height 15
          arena (init-arena width height)]
      (is (not (nil? arena)))
      (is (vector? arena))
      (is (= width (count arena)))
      (is (every? #(= height (count %)) arena))
      (is (every? #(every? nil? %) arena))
    )))
