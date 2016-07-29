(ns weather.fixtures
  "Fixtures and testing data"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :refer [input-stream resource]]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [weather.db :as db]
            [weather.server :refer [connect-to-db]]))


; Testing helpers and data

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


; Fixtures

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
