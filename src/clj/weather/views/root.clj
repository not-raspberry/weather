(ns weather.views.root
  "Root HTML that bootstraps the frontend."
  (:require [config.core :refer [env]]
            [hiccup.page :refer [include-js include-css html5]]))


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

(def root-page
  (html5
    (head)
    [:body.container
     mount-target
     (include-js "/js/app.js")]))
