(ns weather.core
  (:require [clojure.string :as string]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]))

(defonce state (atom {:conditions nil :error nil}))


(defn day-conditions [day-map]
  (let [{:keys [low hi description date]} day-map]
        [:li {:key date}
         (.toString date)
         [:br]
         "Low " low
         [:br]
         "High " hi
         [:br]
         description]))

(defn forecast-page []
  [:div [:h2 "Welcome to weather"]
   (when-let [error (:error @state)]
     (str "Error fetching weather conditions: " error))
   (when-let [conditions (:conditions @state)]
     [:ul
      (map day-conditions conditions)])])

(defn mount-root []
  (reagent/render [forecast-page] (.getElementById js/document "app")))

(defn conditions-fetched [resp]
  (swap! state assoc
         :conditions resp
         :error nil))

(defn conditions-fetch-error [err]
  (swap! state assoc
         :error (str (:status-text err) " " (:status err))))

(defn fetch-conditions []
  (GET "/conditions" {:handler conditions-fetched
                       :error-handler conditions-fetch-error}))

(defn init! []
  (mount-root)
  (fetch-conditions))
