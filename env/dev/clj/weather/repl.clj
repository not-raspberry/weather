(ns weather.repl
  (:require [clojure.java.jdbc :as jdbc]
            [weather.server :as server]
            [weather.db :as db]))

(defonce server (atom nil))
(defonce db nil)

(defn start-server
  "Used for starting the server in development mode from REPL."
  []
  (weather.server/connect-to-db)
  (alter-var-root
    #'db
    (constantly {:connection (jdbc/get-connection {:datasource db/datasource})}))
  (reset! server (weather.server/start-server)))

(defn stop-server []
  (.close (:connection db))
  (db/disconnect!)
  (alter-var-root #'db (constantly nil))
  (.stop @server)
  (reset! server nil))
