(ns weather.test-handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [cognitect.transit :as transit]
            [weather.fixtures :refer [db-setup clean-conditions-table]]
            [weather.handler :refer [app]])
  (:import [java.io ByteArrayInputStream]))


(use-fixtures :once db-setup)
(use-fixtures :each clean-conditions-table)

(defn read-transit-string [transit-string]
  (let [in (ByteArrayInputStream. (.getBytes transit-string "utf-8"))]
    (transit/read (transit/reader in :json-verbose))))

(deftest test-root-resource
  (let [resp (app (mock/request :get "/"))]
    (is (= (:status resp) 200))
    (is (re-find #"^text/html;" (get-in resp [:headers "Content-Type"])))
    (is (re-find #"It's loading\.\.\." (:body resp)))))

(deftest test-conditions-resource
  (let [resp (app (mock/request :get "/conditions"))]
    (is (= (:status resp) 200))
    (is (re-find #"application/transit\+json-verbose;"
                 (get-in resp [:headers "Content-Type"])))
    (is (= (read-transit-string (:body resp))
           {:forecast [], :historical-avg {:hi nil, :low nil}}))))
