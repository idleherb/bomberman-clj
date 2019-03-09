(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena with 1 player should be initialized"
    (let [width 17
          height 15
          players [{:symbol \P} {:symbol \Q}]
          arena (init-arena width height players)]
      (is (not (nil? arena)))
      (is (map? arena))
      (is (contains? arena :grid))
      (let [grid (:grid arena)
            v (:v grid)]
        (is (= (* width height) (count v)))
        (is (= 2 (count (filter (complement nil?) v)))))
      (is (contains? arena :players))
      (let [players (:players arena)]
        (is (seq? players))
        (is (= 2 (count players)))
        (let [player-1 (nth players 0)]
          (is (map? player-1))
          (is (contains? player-1 :coords))
          ))))

  (testing "2 players should spawn on the top left and the bottom right cell of a given grid"
    (let [width 17
          height 15
          grid {:width width, :height height, :v (into (vector) (take (* width height) (repeat nil)))}
          player-1 {:symbol \P, :coords [0 0]}
          player-2 {:symbol \Q, :coords [16 14]}
          grid (spawn grid player-1)
          grid (spawn grid player-2)]
      (is (= \P (nth (:v grid) 0)))
      (is (= \Q (nth (:v grid) 254)))
      )))
