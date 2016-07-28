(ns weather.server
  (:require [clojure.edn :as edn]
            [weather.handler :refer [app]]
            [weather.db :as db]
            [weather.fetch :as fetch]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn coerce-to-int [n]
  (if (string? n)
    (Integer/parseInt n)
    n))

(defn db-options
  "Extract database connection options from the passed env."
  [environment]
  (let [mandatory-db-keys [:database-name :username :password]
        db-options (select-keys env mandatory-db-keys)
        missing-params (remove db-options mandatory-db-keys)]

    (if (empty? missing-params)
      db-options
      (throw (ex-info "Missing keys in the config" {:missing-keys missing-params})))))

(defn connect-to-db []
  (db/connect! (db-options env)))

(defn start-server []
  (run-jetty app {:port (coerce-to-int (:port env 3000))
                  :join? false}))

(defn -main [& args]
  (try
    (connect-to-db)
    (catch clojure.lang.ExceptionInfo e
      (do (println (.getMessage e) (ex-data e))
          (System/exit 1))))

  (if-let [command (first args)]
    (case command
      "migrate" (db/migrate :upgrade)
      "rollback" (db/migrate :rollback)
      "initial-fetch" (fetch/initial-fetch-and-save)
      "fetch" (fetch/fetch-and-save)
      (do (println "Unrecognised command."
                   "Use `migrate`, `rollback` `fetch`, `initial-fetch`.")
          (System/exit 1)))
    (start-server)))
