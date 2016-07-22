(ns weather.handler
  (:require [clojure.tools.logging.impl :as logging-impl]
            [clojure.tools.logging :as logging]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [weather.middleware :refer [wrap-middleware]]
            [weather.db :refer [wrap-db-connection]]
            [config.core :refer [env]]))

; The dependency on hikari-cp adds slf4j with no handlers and tools.logging
; is more than happy to use it. To avoid having to use and configure slf4j,
; force java.util.logging.
(alter-var-root #'logging/*logger-factory*
                (constantly (logging-impl/jul-factory)))

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

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware (wrap-db-connection #'routes)))
