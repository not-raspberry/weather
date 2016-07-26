(ns weather.core
  (:require [clojure.string :as string]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]))

(defonce state (atom {:conditions nil :error nil}))


(defn difference
  "Formats signed difference between value and avg.

  E.g. +1 or -4.
  Returns nil if numbers are equal"
  [value avg]
  (let [diff (- value avg)]
    (if-not (zero? diff)
      (str (if (pos? diff) "+" "-")
           (.abs js/Math diff)))))

(defn day-conditions [day-map average-low average-hi]
  (let [{:keys [low hi description weekday]} day-map]
    [:div.col-md-3 {:key weekday}
     [:div.panel.panel-info
      [:div.panel-heading weekday]
      [:div.panel-body
       "High " hi " " (difference hi average-hi)
       [:br]
       "Low " low " " (difference low average-low)
       [:br]
       description]]]))

(defn forecast-page []
  [:div [:h2 "Weather in London"]
   (when-let [error (:error @state)]
     [:div.alert.alert-danger {:role "alert"}
      (str "Error fetching weather conditions: " error)])
   (when-let [conditions (:conditions @state)]
     (let [{forecast :forecast
            {:keys [hi low]} :historical-avg} conditions]
       [:div
        [:h3 "Forecast"]
        [:hr]
        [:div.row
         (map #(day-conditions % low hi) forecast)]]))])

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
