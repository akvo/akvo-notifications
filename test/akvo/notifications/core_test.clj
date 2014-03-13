(ns akvo.notifications.core-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [akvo.notifications.core :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (handler (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "OK"))))

  (testing "not-found route"
    (let [response (handler (request :get "/invalid"))]
      (is (= (:status response) 404)))))
