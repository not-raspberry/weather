(ns weather.server
  (:require [clojure.edn :as edn]
            [clojure.tools.nrepl :as repl]
            [weather.handler :refer [app]]
            [weather.db :as db]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn read-config [path]
  (edn/read-string (slurp path)))

(defn coerce-to-int [n]
  (if (string? n)
    (Integer/parseInt n)
    n))

(defn -main [& args]
  (let [mandatory-db-keys [:database-name :username :password]
        db-options (select-keys env mandatory-db-keys)
        missing-params (remove db-options mandatory-db-keys)]

    (when (not-empty missing-params)
      (println "Missing keys in the config:" missing-params)
      (System/exit 1))

    (db/connect! db-options)
    (run-jetty app {:port (coerce-to-int (:port env 3000))
                    :join? false})))
