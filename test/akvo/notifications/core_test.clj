(ns akvo.notifications.core-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [clojure.data.json :as json]
            [akvo.notifications.rest-api :as api]))

(deftest test-app
  (testing "Root route"
    (let [response (api/handler (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) (json/write-str api/api-map)))))

  (testing "not-found route"
    (let [response (api/handler (request :get "/invalid"))]
      (is (= (:status response) 404)))))
