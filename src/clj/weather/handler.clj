(ns weather.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [clojure.java.jdbc :as jdbc]
            [weather.middleware :refer [wrap-middleware]]
            [weather.db :refer [*db* wrap-db-connection]]
            [config.core :refer [env]]
            [ring.middleware.transit :refer [wrap-transit-response]]))

(def mount-target
  [:div#app
      [:h3 "It's loading..."]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(def loading-page
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn conditions
  "Responds with recent conditions as EDN."
  [req]
  {:body (jdbc/query
           *db*
           ["SELECT date, description, temp_min, temp_max FROM conditions
            ORDER BY date DESC LIMIT 32"])})

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/conditions" [] conditions)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware (wrap-transit-response
                            (wrap-db-connection #'routes)
                            {:encoding :json-verbose})))
