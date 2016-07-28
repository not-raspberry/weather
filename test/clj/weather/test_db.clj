(ns weather.test-db
  "Forecast fetching and querying tests using the database."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :refer [to-sql-date]]
            [weather.test-fetch :refer [forecast-stream expected-parsed-forecast]]
            [weather.db :as db]
            [weather.server :refer [connect-to-db]]
            [weather.fetch :refer :all]))

(defn db-setup
  "Fixture to connect to the testing database, nuke it, recreate it using
  migrations, and create a default connection."
  [f]
  (connect-to-db)

  ; Cleaning the schema. Using DROP SCHEMA would require making the user
  ; the owner of the schema. To minimize the amount of configuration, tables
  ; are dropped one by one.
  (jdbc/with-db-connection [conn {:datasource @db/datasource}]
    (jdbc/execute!
      conn "DROP TABLE IF EXISTS migrations; DROP TABLE IF EXISTS conditions;"))

  (db/migrate :upgrade)

  (jdbc/with-db-connection [conn {:datasource @db/datasource}]
    (binding [db/*db* conn]
      (f)))

  (db/disconnect!))

(defn clean-conditions-table
  "Deletes everything from the conditions table on setup."
  [f]
  (jdbc/execute! db/*db*  "DELETE FROM conditions")
  (f))

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
