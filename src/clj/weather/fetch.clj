(ns weather.fetch
  "Fetching and saving weather forecasts and faking historical data."
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-local-date to-sql-date]]
            [clj-time.format :as f]
            [weather.db :as db]))

(def weather-url
  "Weather forcecast for London for today and the next 3 days."
  "http://wxdata.weather.com/wxdata/weather/local/UKXX0085?cc=*&unit=m&dayf=4")

(defn grab-forecast-stream []
  (:body (http/get weather-url {:as :stream})))

(defn xml-stream->xml-zip [stream]
  (-> stream
      xml/parse
      zip/xml-zip))

(defn day-element->day-map [day]
  {:hi (Integer/parseInt (zip-xml/xml1-> day :hi zip-xml/text))
   :low (Integer/parseInt (zip-xml/xml1-> day :low zip-xml/text))
   :description (zip-xml/xml1-> day :part (zip-xml/attr= :p "d") :t zip-xml/text)})

(def forecast-time-formatter (f/formatter "M/d/yy"))

(defn forecast-date
  "Extracts the date from the weather API timestamps."
  [formatted-date]
  (to-local-date (f/parse forecast-time-formatter
                     (first (string/split formatted-date #" ")))))

(defn parsed-forecast [forecast]
  (let [days-wrapper (zip-xml/xml1-> forecast :dayf)
        query-time (zip-xml/xml1-> forecast :cc :lsup zip-xml/text)
        days (zip-xml/xml-> days-wrapper :day)]
    {:forecast-day (forecast-date query-time)
     :days (map day-element->day-map days)}))

(defn stamped-forecast-days [parsed-forecast]
  (let [first-date (:forecast-day parsed-forecast)]
    (map-indexed
      (fn [i day]
        (assoc day :date (t/plus first-date (t/days i))))
      (:days parsed-forecast))))

(defn processed-forecast [forecast-stream]
  (-> forecast-stream
      xml-stream->xml-zip
      parsed-forecast
      stamped-forecast-days))

(defn fetch-forecast
  "Fetches the forecast from the API and formats it for database insertion."
  []
  (processed-forecast (grab-forecast-stream)))

(defn rand-range
  "Returns a random integer between `from` (inclusive) and `to` (exclusive)."
  [from to]
  (+ from (rand-int (- to from))))

(defn random-weather
  "Generates fake weather conditions."
  []
  {:hi (rand-range 15 26)
   :low (rand-range 5 16)
   :description nil})

(defn fake-historical-conditions
  "Generates fake historical conditions for 28 days before `day-until`."
  [day-until]
  (for [days-delta (range -28 0)]
    (assoc (random-weather)
           :date
           (t/plus day-until (t/days days-delta)))))

(defn save-conditions
  "Save condition data to the database."
  [conditions]
  (jdbc/with-db-connection [conn {:datasource @db/datasource}]
    (db/save-weather-conditions
      conn
      {:conditions
       (map (juxt (comp to-sql-date :date) :description :low :hi)
            conditions)})))

(defn fetch-and-save
  "Fetches the forecast, formats it, and saves."
  []
  (let [conditions (fetch-forecast)]
    (log/info "Forecast" conditions)
    (save-conditions conditions)
    (log/info "Success.")))

(defn initial-fetch-and-save
  "Fetches the forecast, formats it, prepends with fake historical data, and saves."
  []
  (let [forecast-days (fetch-forecast)
        fake-historical-days (fake-historical-conditions
                               (:date (first forecast-days)))]
    (log/info "Forecast" forecast-days)
    (log/info "Fake historical data" fake-historical-days)
    (save-conditions (concat fake-historical-days forecast-days))
    (log/info "Success.")))
