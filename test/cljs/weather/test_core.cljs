(ns weather.test-core
  (:require [cljs.test :refer-macros [deftest is are testing run-tests]]
            [weather.core :refer [difference]]))


(deftest test-difference
  (are [value avg diff] (= (difference value avg) diff)
       5 0 "-5"
       0 4 "+4"
       0 -4 "-4"
       0 0 nil))
