(ns weather.repl
  "REPL glue code."
  (:require [clojure.java.jdbc :as jdbc]
            [weather.server :as server]
            [weather.db :as db]))

(defonce server (atom nil))
(defonce conn nil)

(defn inject-db-connection [target-var]
  (alter-var-root
    target-var
    (constantly
      {:connection (jdbc/get-connection {:datasource @db/datasource})})))

(defn connect-to-database
  "Connects to the database and injects connections to the local conn var
  for use from this namespace and weather.db/*db* so it is possible to execute
  queries outside the request context."
  []
  (weather.server/connect-to-db)
  ; Local connection to execute queries in this namespace:
  (inject-db-connection #'conn)
  ; Normally #'db/*db* is bound on each request. This enables executing
  ; DB queries in other namespaces from the REPL or REPL-connected editors:
  (inject-db-connection #'db/*db*))

(defn disconnect-from-database
  "Disconnects from the database and cleans up injected connections."
  []
  (.close (:connection conn))
  (.close (:connection db/*db*))
  (db/disconnect!)
  (alter-var-root #'conn (constantly nil)))

(defn start-server
  "Starts the server in development mode from REPL."
  []
  (reset! server (weather.server/start-server)))

(defn stop-server
  "Stops the server in development mode from REPL."
  []
  (.stop @server)
  (reset! server nil))

(defn start
  "Starts the server and the DB connection for development.

  Injects active connections to this namespace and db/*db*."
  []
  (connect-to-database)
  (start-server))

(defn stop
  "Stops the server and the DB connection and cleans up injected connections."
  []
  (disconnect-from-database)
  (stop-server))
