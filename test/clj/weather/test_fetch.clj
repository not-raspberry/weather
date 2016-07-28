(ns weather.test-fetch
  (:require [clojure.java.io :refer [input-stream resource]]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clj-time.core :as t]
            [weather.fetch :refer :all]))

(defn forecast-stream []
  (input-stream (resource "sample/forecast.xml")))

(defn forecast-stream-empty []
  (input-stream (resource "sample/empty-forecast.xml")))

(def expected-parsed-forecast
  "Expected results of parsing sample/forecast.xml"
  [{:hi 23
    :low 14
    :description ""
    :date (t/local-date 2016 7 24)}
   {:hi 23
    :low 12
    :description "Cloudy"
    :date (t/local-date 2016 7 25)}
   {:hi 23
    :low 15
    :description "Mostly Cloudy"
    :date (t/local-date 2016 7 26)}
   {:hi 22
    :low 15
    :description "Showers"
    :date (t/local-date 2016 7 27)}])


(defspec number-generated-by-rand-range-is-in-the-passed-range
  100000
  (prop/for-all [a gen/int b gen/int]
    (let [r (rand-range a b)]
      (or (= a r)       ; the first number is inclusive
          (< a r b)     ; for ascending ranges
          (> a r b))))) ; for descending ranges

(deftest test-rand-range
  (testing "ascending range"
    (is (contains? #{1 2 3} (rand-range 1 4)))
    (is (contains? #{-10 -9 -8 -7} (rand-range -10 -8))))

  (testing "descending range"
    (is (contains? #{4 3 2} (rand-range 4 1)))
    (is (contains? #{-3 -2 -1} (rand-range -3 0))))

  (testing "thin range"
    (is (= 3 (rand-range 3 3)))))

(defn check-weather [weather]
  (is (<= 15 (:hi weather) 25))
  (is (<= 5 (:low weather) 15))
  (is (nil? (:description weather))))

(deftest test-random-weather
  (testing "conditions hi and low temperatures"
    (dotimes [_ 10000]
      (check-weather (random-weather)))))

(deftest test-fake-historical-conditions
  (let [today (t/local-date 2008 11 12)
        conditions (fake-historical-conditions today)]

    (testing "conditions for each day contain valid weather data"
      (doseq [weather conditions] (check-weather weather)))

    (testing "generated days are consecutive"
      (doseq [[prev-day next-day] (partition 2 1 (map :date conditions))]
        (is (t/equal? (t/plus prev-day (t/days 1)) next-day))))

    (is (t/equal? (t/plus (:date (last conditions)) (t/days 1))
                  today) "Last date should be one day before the passed date.")))

(deftest test-forecast-date
  (are [date-str date] (= (forecast-date date-str) date)
       "7/26/16 1:05 AM BST" (t/local-date 2016 7 26)
       "7/24/16 5:45 PM BST" (t/local-date 2016 7 24)))

(deftest test-processed-forecast
  (testing "valid response XML with conditions in it"
    (is (= (processed-forecast (forecast-stream))
           expected-parsed-forecast)))

  (testing "response with no forecast"
    (is (= (processed-forecast (forecast-stream-empty)) []))))
