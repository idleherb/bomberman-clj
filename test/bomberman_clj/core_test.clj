(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena with 1 player should be initialized"
    (let [width 17
          height 15
          arena (init-arena width height)]
      (is (not (nil? arena)))
      (is (map? arena))
      (is (contains? arena :grid))
      (let [grid (:grid arena)]
        (is (= width (count grid)))
        (is (every? #(= height (count %)) grid))
        (is (every? #(every? nil? %) grid)))
      (is (contains? arena :players))
      (let [players (:players arena)]
        (is (vector? players))
        (is (= 2 (count players)))
        )
    )))
