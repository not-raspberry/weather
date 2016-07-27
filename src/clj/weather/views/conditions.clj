(ns weather.views.conditions
  "Wearther conditions resource."
  (:require [weather.db :refer [*db* weather-forecast historical-conditions]]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-sql-date to-local-date]]))


(defn conditions
  "Responds with current, historical and forecast conditions as transit."
  [req]
  (let [current-day-in-london
        (to-local-date (t/to-time-zone (t/now) (t/time-zone-for-id "Europe/London")))
        sql-today (to-sql-date current-day-in-london)
        sql-two-weeks-before (to-sql-date (t/minus current-day-in-london (t/days 28)))
        forecast (weather-forecast *db* {:date-today sql-today})
        {:keys [hi low]} (historical-conditions
                           *db* {:from sql-two-weeks-before :until sql-today})]
    {:body
     {:forecast (vec forecast)
      :historical-avg {:hi hi :low low}}}))
