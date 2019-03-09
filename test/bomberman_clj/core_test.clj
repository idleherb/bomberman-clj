(ns bomberman-clj.core-test
  (:require [clojure.test :refer :all]
            [bomberman-clj.core :refer :all]))

(deftest test-core
  (testing "An empty arena should be initialized"
    (let [arena init-arena]
      (is (not (nil? arena))))))
