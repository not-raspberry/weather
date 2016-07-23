(ns weather.db
  "Database connection pool."
  (:require [hikari-cp.core :as hikari]
            [clojure.java.jdbc :as jdbc]))

(def connection-defaults
  {:auto-commit        true
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


(def datasource)

(defn connect!
  "Establish the connection to the database.

  `connection-options` must contain keys: :database-name, :username, :password
  and may contain any keys accepted by hikari-cp.
  "
  [connection-options]
  (def datasource
    (hikari/make-datasource (merge connection-defaults connection-options))))

(defn disconnect! []
  (hikari/close-datasource datasource))

(def ^:dynamic *db* "Current database connection from the pool." nil)

(defn wrap-db-connection
  "Wraps a request handler to grab a connection from the pool before calling the
  handler and put it back after."
  [handler]
  (fn handler-with-db-connection [request]
    (assert (some? datasource)
            "Requests may only happen after establishing a DB connection.")
    (jdbc/with-db-connection [conn {:datasource datasource}]
      (binding [*db* conn]
        (handler request)))))
