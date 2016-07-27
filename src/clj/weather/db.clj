(ns weather.db
  "Database connection pool and migrations."
  (:require [hikari-cp.core :as hikari]
            [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [clj-time.jdbc]  ; Extends jdbc date conversion protocols.
            [migratus.core :as migratus]))

(def connection-defaults
  {:auto-commit        false
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10
   :pool-name          "db-pool"
   :adapter            "postgresql"
   :server-name        "localhost"
   :port-number        5432
   :register-mbeans    false})

(defonce datasource (atom nil))

(defonce ^:dynamic ^{:doc "Current database connection from the pool."}
  *db* nil)

(hugsql/def-db-fns "sql/queries.sql")


(defn connect!
  "Establish the connection to the database.

  `connection-options` must contain keys: :database-name, :username, :password
  and may contain any keys accepted by hikari-cp.
  "
  [connection-options]
  (reset! datasource
    (hikari/make-datasource (merge connection-defaults connection-options))))

(defn disconnect! []
  (swap! datasource (fn [s] (hikari/close-datasource s) nil)))

(defn wrap-db-connection
  "Wraps a request handler to grab a connection from the pool before calling the
  handler and put it back after."
  [handler]
  (fn handler-with-db-connection [request]
    (let [source @datasource]
      (assert (some? source)
              "Requests may only happen after establishing a DB connection.")
      (jdbc/with-db-connection [conn {:datasource source}]
        (binding [*db* conn]
          (handler request))))))

(defn migrate
  "Migrate the table up (if the command is :upgrade) or revert the last migration
  (if it is :rollback).

  Requires an active db connection."
  [command]
  (assert (some? datasource)
          "Migrations require the DB connection to be set up.")
  (jdbc/with-db-connection [conn {:datasource @datasource}]
    (let [config
          {:store :database
           :migration-dir "migrations/"
           :migration-table-name "migrations"
           :db conn}]
      (case command
        :upgrade (migratus/migrate config)
        :rollback (migratus/rollback config)))))
