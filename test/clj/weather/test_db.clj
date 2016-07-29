(ns weather.test-db
  "Forecast fetching and querying tests using the database."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :refer [to-sql-date]]
            [weather.fixtures :refer [db-setup clean-conditions-table
                                      forecast-stream expected-parsed-forecast]]
            [weather.db :as db]
            [weather.server :refer [connect-to-db]]
            [weather.fetch :refer :all]))

(use-fixtures :once db-setup)
(use-fixtures :each clean-conditions-table)


(def weekday-formatter (f/formatter-local "EEEE"))

(defn weekday [date today]
  (if (t/equal? date today)
    "Today"
    (f/unparse-local weekday-formatter date)))

(defn parsed-api-response->expected-db-response
  "Transforms parsed forecast response from the API to what we expect to get
  from the database query."
  [{date :date :as day-conditions} today]
  (-> day-conditions
      (assoc :weekday (weekday date today))
      (dissoc :date)))


(deftest test-conditions-data-upsert
  (let [date (t/local-date 2016 7 27)

        day-conditions-no-description
        {:hi 22 :low 15 :description "" :date date}

        day-conditions-warmer-with-description
        (assoc day-conditions-no-description
               :hi 24 :low 18 :description "Quite nice")

        day-conditions-warmer-no-description
        (assoc day-conditions-warmer-with-description :description "")

        fetch-forecast
        (fn [] (db/weather-forecast
                 db/*db* {:date-today (to-sql-date date)}))]

    (testing "Conditions are saved to the DB"
      (save-conditions [day-conditions-no-description])
      (is (= (fetch-forecast)
             [(parsed-api-response->expected-db-response
                day-conditions-no-description date)])))

    (testing "Can update the row."
      (save-conditions [day-conditions-warmer-with-description])
      (is (= (fetch-forecast)
             [(parsed-api-response->expected-db-response
                day-conditions-warmer-with-description date)])))

    (testing "Updating the row with empty description leaves the original description."
      (save-conditions [day-conditions-warmer-no-description])
      (is (= (fetch-forecast)
             [(parsed-api-response->expected-db-response
                day-conditions-warmer-with-description date)])))))

(deftest test-conditions-data-query-time
  (let [date (t/local-date 2014 1 3)
        query-date-2-days-before (t/minus date (t/days 2))
        query-date-too-late (t/plus date (t/days 1))
        day-conditions {:hi 22 :low 15 :description "Sth." :date date}]

    (save-conditions [day-conditions])

    (testing "Future conditions should be reported"
      (doseq [query-date [date query-date-2-days-before]]
        (is (= (db/weather-forecast db/*db* {:date-today
                                             (to-sql-date query-date)})
               [(parsed-api-response->expected-db-response
                  day-conditions query-date)]))))

    (testing "Past conditions should be ignored"
      (is (= (db/weather-forecast db/*db* {:date-today
                                           (to-sql-date query-date-too-late)})
             [])))))

(deftest test-fetch-save-query-round-trip
  (doseq [[fetching-fn expected-historical-rows-count
           expected-historical-temperatures]
          [[fetch-and-save 0 {:hi nil :low nil}]
           [initial-fetch-and-save 28 {:hi 20 :low 10}]]]

    (with-redefs [grab-forecast-stream forecast-stream
                  random-weather (constantly
                                   {:hi 20
                                    :low 10
                                    :description nil})]
      (fetching-fn)

      (testing "Conditions rows count"
        (is (= (:count (first (jdbc/query
                                db/*db* "SELECT COUNT(*) FROM conditions")))
               (+ (count expected-parsed-forecast) expected-historical-rows-count))))

      (let [date-today (:date (first expected-parsed-forecast))
            two-weeks-ago (t/minus date-today (t/days 28))
            expected-query-result
            (map
              #(parsed-api-response->expected-db-response % date-today)
              expected-parsed-forecast)]

        (testing "Forecast query results"
          (is (= (db/weather-forecast
                   db/*db* {:date-today (to-sql-date date-today)})
                 expected-query-result)))

        (testing "Historical query results"
          (is (= (db/historical-conditions
                   db/*db* {:from (to-sql-date two-weeks-ago)
                            :until (to-sql-date date-today)})
                 expected-historical-temperatures)))))))
