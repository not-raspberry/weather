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
      {:connection (jdbc/get-connection {:datasource db/datasource})})))

(defn connect-to-database []
  (weather.server/connect-to-db)
  ; Local connection to execute queries in this namespace:
  (inject-db-connection #'conn)
  ; Normally #'db/*db* is bound on each request. This enables executing
  ; DB queries in other namespaces from the REPL or REPL-connected editors:
  (inject-db-connection #'db/*db*))

(defn disconnect-from-database []
  (.close (:connection conn))
  (.close (:connection db/*db*))
  (db/disconnect!)
  (alter-var-root #'conn (constantly nil)))

(defn start-server
  "Used for starting the server in development mode from REPL."
  []
  (reset! server (weather.server/start-server)))

(defn stop-server []
  (.stop @server)
  (reset! server nil))

(defn start []
  (connect-to-database)
  (start-server))

(defn stop []
  (disconnect-from-database)
  (stop-server))
