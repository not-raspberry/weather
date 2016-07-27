-- :name weather-forecast :?
/*
:doc Query forecast for the days since :date-today, which is the current date
  in the queried timezone.
  Returned columns are
    :weekday - 'Today' or name of the week day,
    :description - Human-readable conditions description
    :low - min temperature
    :hi - max temperature
*/
SELECT
    CASE WHEN date = :date-today THEN 'Today'
               ELSE trim(trailing ' ' from to_char(date, 'Day'))
    END AS weekday,
    description,
    temp_min AS low,
    temp_max as hi
FROM conditions
WHERE date >= :date-today
ORDER BY date


-- :name historical-conditions :? :1
/*
:doc Query the range from :from (inclusive) until :until (exclusive) for average
  daily min and max temperatures, returned as :low and :hi keys.
*/
SELECT
    round(avg(temp_min)) :: INTEGER AS low,
    round(avg(temp_max)) :: INTEGER as hi
FROM conditions
WHERE date < :until AND date >= :from


-- :name save-weather-conditions :i! :n
/*
:doc Upsert daily conditions into the conditions table.
  Values for each day are in order: date, description, temp_min, temp_max

  The description column will be updated only if the new value is not empty.
*/
INSERT INTO conditions (date, description, temp_min, temp_max)
VALUES :tuple*:conditions
ON CONFLICT (date) DO UPDATE SET
    description =
        CASE WHEN EXCLUDED.description = ''
            THEN conditions.description
        ELSE EXCLUDED.description
        END,
        temp_min = EXCLUDED.temp_min,
        temp_max = EXCLUDED.temp_max
