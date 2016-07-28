(ns weather.test-core
  (:require [cljs.test :refer-macros [deftest is are testing run-tests]]
            [weather.core :refer [formatted-difference]]))


(deftest test-difference
  (are [avg value diff] (= (formatted-difference avg value) diff)
       5 0 "-5"
       0 4 "+4"
       0 -4 "-4"
       nil nil nil
       1 nil nil
       nil 1 nil
       0 0 nil))
