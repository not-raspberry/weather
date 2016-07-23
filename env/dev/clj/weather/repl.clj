(ns weather.repl
  (:require [weather.server :as server]
            [weather.db :as db]))

(defonce server (atom nil))

(defn start-server
  "Used for starting the server in development mode from REPL."
  []
  (weather.server/connect-to-db)
  (reset! server (weather.server/start-server)))

(defn stop-server []
  (db/disconnect!)
  (.stop @server)
  (reset! server nil))
