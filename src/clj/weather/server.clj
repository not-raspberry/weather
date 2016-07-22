(ns weather.server
  (:require [weather.handler :refer [app]]
            [weather.db :as db]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (db/connect!
    {:database-name      "weather"
     :username           "weather"
     :password           "dev"})
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (run-jetty app {:port port :join? false})))
