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
        (is (= (* width height) (count grid)))
        (is (every? nil? grid)))
      (is (contains? arena :players))
      (let [players (:players arena)]
        (is (vector? players))
        (is (= 2 (count players))))))

  (testing "A player should be spawned on an empty cell of a given arena grid"
    (let [width 17
          height 15
          grid (into (vector) (take (* width height) (repeat nil)))
          coords-0-0 [0 0]
          coords-16-14 [16 14]
          grid (spawn grid coords-0-0)
          grid (spawn grid coords-16-14)]
      (is (not (nil? (nth grid 0))))
      (is (not (nil? (nth grid 254))))
      )))