(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty (17 x 15) arena with 2 players and 0 bombs should be initialized"
    (let [width 1
          height 2
          players {:player-1 {:glyph \P} :player-2 {:glyph \Q}}
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
        (is (map? players))
        (is (= 2 (count players)))
        (let [player-1 (:player-1 players)
              player-2 (:player-2 players)]
          (is (map? player-1))
          (is (= \P (:glyph player-1)))
          (is (contains? player-1 :coords))
          (is (map? player-2))
          (is (= \Q (:glyph player-2)))
          (is (contains? player-2 :coords)))
      (is (contains? arena :bombs))
      (let [bombs (:bombs arena)]
        (is (map? bombs))
        (is (= 0 (count bombs)))))))

  (testing "3 players should spawn in the correct positions"
    (let [width 17
          height 15
          grid {:width width, :height height, :v (into (vector) (take (* width height) (repeat nil)))}
          grid (spawn grid :player-1 {:glyph \P} [0 0])
          grid (spawn grid :player-2 {:glyph \Q} [12 7])
          grid (spawn grid :player-3 {:glyph \R} [16 14])
          cell-player-1 (nth (:v grid) 0)
          cell-player-2 (nth (:v grid) 131)
          cell-player-3 (nth (:v grid) 254)]
      (is (map? cell-player-1))
      (is (contains? cell-player-1 :player-1))
      (is (map? cell-player-2))
      (is (contains? cell-player-2 :player-2))
      (is (map? cell-player-3))
      (is (contains? cell-player-3 :player-3))))

  (testing "Grid cells should be identified correctly"
    (let [width 2
          height 3
          grid {:width width, :height height, :v [0 1 nil 3 4 5]}]
      (is (= 0 (cell-at grid [0 0])))
      (is (= 1 (cell-at grid [1 0])))
      (is (cell-empty? grid [0 1]))
      (is (= 3 (cell-at grid [1 1])))
      (is (= 4 (cell-at grid [0 2])))
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
      (is (= 1 (cell-at grid [1 0])))
      (is (cell-empty? grid [0 1]))
      (is (= 3 (cell-at grid [1 1])))
      (is (cell-empty? grid [0 2]))
      (is (= 5 (cell-at grid [1 2])))))

  (testing "An random empty cell should be found"
    (let [width 3
          height 3
          grid {:width width, :height height, :v [1 1 1 1 1 1 1 nil 1]}]
      (is (= [1 2] (find-empty-cell grid)))))

  (testing "A player can move to NESW empty cells within grid"
    (let [width 3
          height 3
          players {:player-1 {:glyph \P :coords [1 1]}}
          arena (init-arena width height players)
          arena (move arena :player-1 :south)
          arena (move arena :player-1 :south)  ; hit wall
          arena (move arena :player-1 :west)
          arena (move arena :player-1 :west)  ; hit wall
          arena (move arena :player-1 :west)  ; hit wall
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)
          arena (move arena :player-1 :north)  ; hit wall
          arena (move arena :player-1 :north)  ; hit wall
          {players :players {v :v} :grid} arena
          {[x y] :coords} (:player-1 players)]
      (is (= 0 x))
      (is (= 0 y))
      (is (nil? (nth v 4)))))

  (testing "Navigating from coordinates should succeed"
    (let [coords [1 1]
          coords (navigate coords :south)
          coords (navigate coords :west)
          coords (navigate coords :north)
          coords (navigate coords :north)
          [x y] coords]
      (is (= 0 x))
      (is (= 0 y))))

  (testing "Navigating with invalid direction should fail"
    (let [coords [1 1]]
      (is (thrown-with-msg? Exception #"invalid direction:" (navigate coords :down)))))

  (testing "player-1 should place a bomb at in their current position"
    (let [v [{:glyph \P}]
          arena {:grid {:width 1, :height 1, :v v}
                :players {:player-1 {:glyph \P, :coords [0 0]}}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} (plant-bomb arena :player-1)
          cell (first v)]
      (is (= 1 (count bombs)))
      (let [bomb ((keyword (str "x" 0 "y" 0)) bombs)]
        (is (map? bomb))
        (is (= (:player-id bomb) :player-1))
        (is (contains? bomb :coords))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))
      (is (contains? cell :bomb))
      (let [bomb (:bomb cell)]
        (is (map? bomb))
        (is (= (:player-id bomb) :player-1))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))))

  (testing "a planted bomb should still be there after the player moves away"
    (let [v [{:glyph \P} nil]
          arena {:grid {:width 1, :height 2, :v v}
                  :players {:player-1 {:glyph \P, :coords [0 0]}}}
          arena (plant-bomb arena :player-1)
          {{v :v, :as grid} :grid, :as arena} (move arena :player-1 :south)
          cell (first v)]
      (is (contains? cell :bomb))
      (let [bomb (:bomb cell)]
        (is (map? bomb))
        (is (= (:player-id bomb) :player-1))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))))

  (testing "an evaluated arena without any bombs should have no changes"
    (let [v [nil nil nil nil {:glyph \P} nil nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:glyph \P, :coords [1 1]}}}
          evaluated-arena (eval-arena arena)]
      (is (= evaluated-arena arena))))

  ; (testing "an evaluated arena with a bomb should have a changed bomb timestamp"
  ;   (let [v [{:bomb {:player-id :player-1, :timestamp (System/currentTimeMillis)}} nil nil
  ;            nil {:glyph \P} nil
  ;            nil nil nil]
  ;         arena {:grid {:width 3, :height 3, :v v}
  ;                :players {:player-1 {:glyph \P, :coords [1 1]}}}
  ;         evaluated-arena (eval-arena arena)]
  ;     (is (not= evaluated-arena arena))))
)
