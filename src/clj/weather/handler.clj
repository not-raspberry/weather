(ns weather.handler
  (:require [clojure.java.jdbc :as jdbc]
            [weather.middleware :refer [wrap-middleware]]
            [weather.db :refer [*db* wrap-db-connection]]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-sql-date to-local-date to-date]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
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
   (for [asset ["/css/bootstrap" "/css/bootstrap-theme"]]
     (include-css (str asset (if (env :dev) ".css" ".min.css"))))])

(def loading-page
  (html5
    (head)
    [:body.container
     mount-target
     (include-js "/js/app.js")]))

(def historical-conditions-sql
  "SELECT round(avg(temp_min)) :: INTEGER AS low,
          round(avg(temp_max)) :: INTEGER as hi
   FROM conditions
   WHERE date < ? AND date >= ?;")

(def forecast-sql
  "SELECT CASE WHEN date = ? THEN 'Today'
               ELSE trim(trailing ' ' from to_char(date, 'Day'))
          END AS weekday,
          description,
          temp_min AS low,
          temp_max as hi
   FROM conditions
   WHERE date >= ?
   ORDER BY date")

(defn conditions
  "Responds with current, historical and forecast conditions as transit."
  [req]
  (let [current-day-in-london
        (to-local-date (t/to-time-zone (t/now) (t/time-zone-for-id "Europe/London")))
        sql-today (to-sql-date current-day-in-london)
        sql-two-weeks-before (to-sql-date (t/minus current-day-in-london (t/days 28)))
        forecast (jdbc/query *db* [forecast-sql sql-today sql-today])
        [{:keys [hi low]}] (jdbc/query *db* [historical-conditions-sql
                                           sql-today sql-two-weeks-before])]
    {:body
     {:forecast (vec forecast)
      :historical-avg {:hi hi :low low}}}))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/conditions" [] conditions)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware (wrap-transit-response
                            (wrap-db-connection #'routes)
                            {:encoding :json-verbose})))
