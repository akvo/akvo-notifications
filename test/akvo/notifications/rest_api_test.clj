(ns akvo.notifications.rest-api-test
  (:require [akvo.notifications.rest-api :as api]
            [akvo.notifications.data-store :as data]
            [ring.mock.request :refer :all]
            [cheshire.core :as json]
            [clojure.test :refer :all]))

(deftest service
  (testing "GET: /services/1"
    (let [service-fixture ((keyword "1")
                           (data/tuple-vec->id-tuple @data/services-data))
          response (api/handler (request :get "/services/1"))
          service (json/parse-string (:body response) true)]
      (is (= (:status response) 200))
      (is (= service service-fixture)))))

(deftest services-coll
  (testing "GET: /services"
    (let [services-fixture @data/services-data
          response (api/handler (request :get "/services"))
          services (json/parse-string (:body response) true)]
      (is (= (:status response) 200))
      (is (= services services-fixture)))))
