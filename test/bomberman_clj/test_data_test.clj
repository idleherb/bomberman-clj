(ns bomberman-clj.test-data-test
  (:require [midje.sweet :refer [fact facts =>]]
            [bomberman-clj.specs :as specs]
            [bomberman-clj.test-data :refer [make-timestamp]]))

(facts "about test data"
  (fact "timestamps conform to their spec"
    (make-timestamp) => #(specs/valid? ::specs/timestamp %)))
