(ns weather.handler
  (:require [weather.middleware :refer [wrap-middleware]]
            [weather.db :refer [*db* wrap-db-connection]]
            [weather.views.conditions :refer [conditions]]
            [weather.views.root :refer [root-page]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.transit :refer [wrap-transit-response]]))


(defroutes routes
  (GET "/" [] root-page)
  (GET "/conditions" [] conditions)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware (wrap-transit-response
                            (wrap-db-connection #'routes)
                            {:encoding :json-verbose})))
