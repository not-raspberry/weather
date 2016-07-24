(ns weather.fetch
  (:require [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-local-date]]
            [clj-time.format :as f]))

(def weather-url
  "Weather forcecast for London for today and the next 3 days."
  "http://wxdata.weather.com/wxdata/weather/local/UKXX0085?cc=*&unit=m&dayf=4")

(defn grab-forecast-xml []
  (-> (http/get weather-url {:as :stream})
      :body
      xml/parse
      zip/xml-zip))

(defn day-element->day-map [day]
  {:hi (Integer/parseInt (zip-xml/xml1-> day :hi zip-xml/text))
   :low (Integer/parseInt (zip-xml/xml1-> day :low zip-xml/text))
   :description (zip-xml/xml1-> day :part (zip-xml/attr= :p "d") :t zip-xml/text)})

(defn parsed-forecast [forecast]
  (let [days-wrapper (zip-xml/xml1-> forecast :dayf)
        query-time (zip-xml/xml1-> forecast :cc :lsup zip-xml/text)
        days (zip-xml/xml-> days-wrapper :day)]
    {:forecast-day (forecast-date query-time)
     :days (map day-element->day-map days)}))

(def forecast-time-formatter (f/formatter "M/d/yy"))

(defn forecast-date
  "Extracts the date from the weather API timestamps."
  [formatted-date]
  (to-local-date (f/parse forecast-time-formatter
                     (first (string/split formatted-date #" ")))))

(defn stamped-forecast-days [parsed-forecast]
  (let [first-date (:forecast-day parsed-forecast)]
    (map-indexed
      (fn [i day]
        (assoc day :date (t/plus first-date (t/days i))))
      (:days parsed-forecast))))

(defn fetch-forecast
  "Fetches the forecast from the API and formats it for database insertion."
  []
  (-> (grab-forecast-xml)
      parsed-forecast
      stamped-forecast-days))

(defn rand-range
  "Returns a random integer between `from` (inclusive) and `to` (exclusive)."
  [from to]
  (+ from (rand-int (- to from))))

(defn random-weather
  "Generates fake weather conditions."
  []
  {:hi (rand-range 15 26)
   :low (rand-range 5 16)
   :description (rand-nth ["Mostly sunny" "Mostly cloudy" "Showers" "Sunny"
                           "Cloudy" "Clouds Early / Clearing Late"])})

(defn fake-historical-conditions
  "Generates fake historical conditions for 28 days before `day-until`."
  [day-until]
  (for [days-delta (range -28 0)]
    (assoc (random-weather)
           :date
           (t/plus day-until (t/days days-delta)))))

(defn initial-fetch-forecast
  "Fetches the forecast, formats it and prepends with fake historical data."
  []
  (let [forecast-days (fetch-forecast)
        fake-historical-days (fake-historical-conditions
                               (:date (first forecast-days)))]
    (concat fake-historical-days forecast-days)))
