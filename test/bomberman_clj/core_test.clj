(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena with 1 player should be initialized"
    (let [width 1
          height 2
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
        (is (vector? players))
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
      (is (= \Q (nth (:v grid) 254)))))

  (testing "Grid cells should be identified correctly"
    (let [width 2
          height 3
          grid {:width width, :height height, :v [0 1 nil 3 4 5]}]
      (is (= 0 (cell-at grid [0 0])))
      (is (= 1 (cell-at grid [0 1])))
      (is (cell-empty? grid [0 2]))
      (is (= 3 (cell-at grid [1 0])))
      (is (= 4 (cell-at grid [1 1])))
      (is (= 5 (cell-at grid [1 2])))))

  (testing "A pair of random coordinates should be found"
    (let [width 2
          height 3
          grid {:width width, :height height, :v [nil nil nil nil nil nil]}
          [x y] (rand-coords grid)]
      (is (integer? x))
      (is (integer? y))
      (is (<= 0 x (- width 1)))
      (is (<= 0 y (- height 1)))))

  (testing "Grid cells should be identified correctly"
    (let [width 2
          height 3
          grid {:width width, :height height, :v [0 1 nil 3 nil 5]}]
      (is (= 0 (cell-at grid [0 0])))
      (is (= 1 (cell-at grid [0 1])))
      (is (cell-empty? grid [0 2]))
      (is (= 3 (cell-at grid [1 0])))
      (is (cell-empty? grid [1 1]))
      (is (= 5 (cell-at grid [1 2])))))

  (testing "An random empty cell should be found"
    (let [width 3
          height 3
          grid {:width width, :height height, :v [1 1 1 1 1 1 1 nil 1]}]
      (is (= [2 1] (find-empty-cell grid))))))
