(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(def ts-now 1552767537306)

(deftest test-core
  (testing "An empty (17 x 15) arena with 2 players and 0 bombs should be initialized"
    (let [width 1
          height 2
          players {:player-1 {:glyph \P} :player-2 {:glyph \Q}}
          arena (init-arena width height players)
          {{v :v, :as grid} :grid, players :players, bombs :bombs} arena]
      (is (= (* width height) (count v)))
      (is (= 2 (count (filter (complement nil?) v))))
      (is (map? players))
      (is (= 2 (count players)))
      (let [coords-1 (:player-1 players)
            coords-2 (:player-2 players)]
        (is (vector? coords-1))
        (is (= 2 (count coords-1)))
        (is (vector? coords-2))
        (is (= 2 (count coords-2))))
      (is (map? bombs))
      (is (= 0 (count bombs)))))

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
          grid {:width width
                :height height
                :v [0   1
                    nil 3
                    4   5]}]
      (is (= 0 (cell-at grid [0 0])))
      (is (= 1 (cell-at grid [1 0])))
      (is (cell-empty? grid [0 1]))
      (is (= 3 (cell-at grid [1 1])))
      (is (= 4 (cell-at grid [0 2])))
      (is (= 5 (cell-at grid [1 2])))))

  (testing "A pair of random coordinates should be found"
    (let [width 2
          height 3
          grid {:width width
                :height height
                :v [nil nil
                    nil nil
                    nil nil]}
          [x y] (rand-coords grid)]
      (is (integer? x))
      (is (integer? y))
      (is (<= 0 x (- width 1)))
      (is (<= 0 y (- height 1)))))

  (testing "Grid cells should be identified correctly"
    (let [width 2
          height 3
          grid {:width width
                :height height
                :v [0   1
                    nil 3
                    nil 5]}]
      (is (= 0 (cell-at grid [0 0])))
      (is (= 1 (cell-at grid [1 0])))
      (is (cell-empty? grid [0 1]))
      (is (= 3 (cell-at grid [1 1])))
      (is (cell-empty? grid [0 2]))
      (is (= 5 (cell-at grid [1 2])))))

  (testing "An random empty cell should be found"
    (let [width 3
          height 3
          grid {:width width
                :height height
                :v [1 1   1
                    1 1   1
                    1 nil 1]}]
      (is (= [1 2] (find-empty-cell grid)))))

  (testing "A player can move to NESW empty cells within grid"
    (let [plr {:player-1 {:glyph \P}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          players {:player-1 [1 1]}
          arena {:bombs {} :grid {:width 3 :height 3 :v v} :players players}
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
          [x y, :as coords] (:player-1 players)]
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
    (is (thrown-with-msg? Exception #"invalid direction:" (navigate [0 0] :down))))

  (testing "player-1 should place a bomb at in their current position"
    (let [v [{:player-1 {:glyph \P}}]
          arena {:grid {:width 1, :height 1, :v v}
                 :players {:player-1 [0 0]}}
          arena (plant-bomb arena :player-1)
          {{v :v, :as grid} :grid, bombs :bombs, :as arena} arena
          cell (first v)]
      (is (= 1 (count bombs)))
      (let [bomb ((keyword (str "x" 0 "y" 0)) bombs)]
        (is (map? bomb))
        (is (contains? bomb :coords))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))
      (is (contains? cell :bomb))
      (let [bomb (:bomb cell)]
        (is (map? bomb))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))))

  (testing "a planted bomb should still be there after the player moves away"
    (let [v [{:player-1 {:glyph \P}} nil]
          arena {:grid {:width 1, :height 2, :v v}
                 :players {:player-1 [0 0]}}
          arena (plant-bomb arena :player-1)
          {{v :v, :as grid} :grid, :as arena} (move arena :player-1 :south)
          cell (first v)]
      (is (contains? cell :bomb))
      (let [bomb (:bomb cell)]
        (is (map? bomb))
        (is (not (nil? (re-matches #"\d{13}" (str (:timestamp bomb)))))))))

  (testing "a detonating bomb should spread horizontally and vertically, passing by the offset player"
    (let [bom {:bomb {:timestamp 0}}
          plr {:player-1 {:glyph \P}}
          v [nil nil nil nil nil
             nil bom nil nil nil
             nil nil plr nil nil
             nil nil nil nil nil]
          bomb-id :x1y1
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 [2 2]}
                 :bombs {bomb-id {:timestamp 0, :coords [1 1]}}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena}
            (detonate-bomb arena bomb-id)]
        (is (empty? bombs))
        (is (not (contains? (nth v 6) :bomb)))
        (is (contains? (nth v 12) :player-1))
        (are [cell] (contains? cell :fire)
          (nth v 1)
          (nth v 5)
          (nth v 7)
          (nth v 8)
          (nth v 11)
          (nth v 16))
        (are [cell] (not (contains? cell :fire))
          (nth v 0)
          (nth v 2)
          (nth v 3)
          (nth v 4)
          (nth v 9)
          (nth v 10)
          (nth v 12)
          (nth v 14)
          (nth v 15)
          (nth v 17)
          (nth v 18)
          (nth v 19))))

  (testing "a detonating bomb should spread until the nearby player and stop there"
    (let [bom {:bomb {:timestamp 0}}
          plr {:player-1 {:glyph \P}}
          v [nil nil nil nil nil
             nil bom nil nil nil
             nil plr nil nil nil
             nil nil nil nil nil]
          bomb-id :x1y1
          arena {:grid {:width 5, :height 4, :v v}
                 :players {:player-1 [1 3]}
                 :bombs {bomb-id {:timestamp 0, :coords [1 1]}}}
          {{v :v, :as grid} :grid, bombs :bombs, :as arena}
            (detonate-bomb arena bomb-id)]
        (is (empty? bombs))
        (is (not (contains? (nth v 6) :bomb)))
        (is (not (nil? (:player-1 (nth v 11)))))
        (are [cell] (contains? cell :fire)
          (nth v 1)
          (nth v 5)
          (nth v 7)
          (nth v 8)
          (nth v 11))
        (are [cell] (not (contains? cell :fire))
          (nth v 0)
          (nth v 2)
          (nth v 3)
          (nth v 4)
          (nth v 9)
          (nth v 10)
          (nth v 12)
          (nth v 14)
          (nth v 15)
          (nth v 16)
          (nth v 17)
          (nth v 18)
          (nth v 19))))

  (testing "an evaluated arena without any bombs should have no changes"
    (let [plr {:player-1 {:glyph \P}}
          v [nil nil nil
             nil plr nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:glyph \P, :coords [1 1]}}
                 :bombs {}}
          evaluated-arena (eval-arena arena ts-now)]
      (is (= evaluated-arena arena))))

  (testing "an evaluated arena with an expired bomb should contain fire"
    (let [bom {:bomb {:timestamp 0}}
          plr {:player-1 {:glyph \P}}
          v [bom nil nil
             nil plr nil
             nil nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:glyph \P, :coords [1 1]}}
                 :bombs {:x0y0 {:timestamp 0, :coords [0 0]}}}
          {{v :v, :as grid} :grid
           {player-1 :player-1} :players
           bombs :bombs
           :as evaluated-arena} (eval-arena arena ts-now)]
      (is (not= evaluated-arena arena))
      (are [cell] (contains? cell :fire)
        (nth v 0)
        (nth v 1)
        (nth v 2)
        (nth v 3)
        (nth v 6))
      (are [cell] (not (contains? cell :fire))
        (nth v 4)
        (nth v 5)
        (nth v 7)
        (nth v 8))
      (is (not (contains? (nth v 0) :bomb)))
      (is (not (contains? player-1 :hit)))
      (is (= 0 (count bombs)))))

  (testing "an evaluated arena with an expired bomb should hit the nearby player"
    (let [bom {:bomb {:timestamp 0}}
          plr {:player-1 {:glyph \P}}
          v [bom nil nil
             nil nil bom
             plr nil nil]
          arena {:grid {:width 3, :height 3, :v v}
                 :players {:player-1 {:glyph \P, :coords [0 2]}}
                 :bombs {:x0y0 {:timestamp 0, :coords [0 0]}
                         :x2y1 {:timestamp 0, :coords [2 1]}}}
          {{v :v, :as grid} :grid
           {player-1 :player-1} :players
           bombs :bombs
           :as evaluated-arena} (eval-arena arena ts-now)]
      (is (not= evaluated-arena arena))
      (are [cell] (contains? cell :fire)
        (nth v 0)
        (nth v 1)
        (nth v 2)
        (nth v 3)
        (nth v 4)
        (nth v 6)
        (nth v 5)
        (nth v 8))
      (are [cell] (not (contains? cell :fire))
        (nth v 7))
      (is (not (contains? (nth v 0) :bomb)))
      (is (not (contains? (nth v 5) :bomb)))
      (is (contains? player-1 :hit))
      (is (= 0 (count bombs)))))

  ; (testing "Bomb detonations should propagate to nearby bombs, leaving others unchanged"
  ;   (let [bm1 {:bomb :x0y0}
  ;         bm2 {:bomb :x2y0}
  ;         bm3 {:bomb :x1y2}
  ;         plr {:player-1 {:glyph \P}}
  ;         v [bm1 nil bm2
  ;            nil plr nil
  ;            nil bm3 nil]
  ;         arena {:grid {:width 3, :height 3, :v v}
  ;                :players {:player-1 {:glyph \P, :coords [0 2]}}
  ;                :bombs {:x0y0 {:timestamp 1000000000000, :coords [0 0]}
  ;                        :x2y0 {:timestamp 9999999999999, :coords [2 0]}
  ;                        :x1y2 {:timestamp 9999999999999, :coords [1 2]}}}
  ;         {{v :v, :as grid} :grid
  ;           {player-1 :player-1} :players
  ;           bombs :bombs
  ;           :as evaluated-arena} (eval-arena arena ts-now)]
  ;     (is (not= evaluated-arena arena))
  ;     (are [cell] (contains? cell :fire)
  ;       (nth v 0)
  ;       (nth v 1)
  ;       (nth v 2)
  ;       (nth v 3)
  ;       (nth v 4)
  ;       (nth v 6)
  ;       (nth v 5)
  ;       (nth v 8))
  ;     (are [cell] (not (contains? cell :fire))
  ;       (nth v 7))
  ;     (is (not (contains? (nth v 0) :bomb)))
  ;     (is (not (contains? (nth v 5) :bomb)))
  ;     (is (contains? player-1 :hit))
  ;     (is (= 0 (count bombs)))))
)
