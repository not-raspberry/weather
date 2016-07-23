(ns weather.server
  (:require [clojure.edn :as edn]
            [clojure.tools.logging.impl :as logging-impl]
            [clojure.tools.logging :as logging]
            [weather.handler :refer [app]]
            [weather.db :as db]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

; The dependency on hikari-cp adds slf4j with no handlers and tools.logging
; is more than happy to use it. To avoid having to use and configure slf4j,
; force java.util.logging.
(alter-var-root #'logging/*logger-factory*
                (constantly (logging-impl/jul-factory)))

(defn read-config [path]
  (edn/read-string (slurp path)))

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
    (db/migrate command)
    (start-server)))
