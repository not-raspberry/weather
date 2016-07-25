(ns weather.core
    (:require [reagent.core :as reagent :refer [atom]]))


(defonce state (atom {}))


(defn forecast-page []
  [:div [:h2 "Welcome to weather"]
   [:div "That's about it..."]])

(defn mount-root []
  (reagent/render [forecast-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
