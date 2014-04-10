;;  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
;;
;;  This file is part of Akvo Notifications.
;;
;;  Akvo Notifications is free software: you can redistribute it and modify it
;;  under the terms of the GNU Affero General Public License (AGPL) as published
;;  by the Free Software Foundation, either version 3 of the License or any
;;  later version.
;;
;;  Akvo Notifications is distributed in the hope that it will be useful, but
;;  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
;;  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
;;  License included below for more details.
;;
;;  The full license text can also be seen at
;;  <http://www.gnu.org/licenses/agpl.html>.

(ns akvo.notifications.api-test
  (:require [akvo.notifications.api :as api]
            [clojure.test :refer :all]
            [clojure.edn :as edn]
            [ring.mock.request :refer :all]
            ))

(deftest root-route
  (testing "Make sure we get a response"

    (let [response (api/app-routes (-> (request :get "/")
                                       (header "accept" "application/edn")))]
      (is (= (:status response) 200))
      (is (= (edn/read-string (:body response)) api/api-map)))))

(deftest not-found
  (testing "A non existing route"
    (let [response (api/app-routes (request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest notifications-coll
  (testing "Make sure we get a 404 response"
    (let [response (api/app-routes (request :get "/notifications"))]
      (is (= (:status response) 404)))))

(deftest notifications-single
  (testing "Make sure we get a 404 response"
    (let [response (api/app-routes (request :get "/notifications/1"))]
      (is (= (:status response) 404)))))

;; We need to deal with the components!
;;

;; (deftest services-coll
;;   (testing "Supported media types"
;;     (let [json-response (api/app-routes
;;                          (-> (request :get "/services")
;;                              (header "accept" "application/json")))
;;           xml-response (api/app-routes
;;                         (-> (request :get "/services")
;;                             (header "accept" "application/xml")))]
;;       (is (= (:status json-response) 200))
;;       (is (= (:status xml-response) 406)))))


;; (deftest services-single
;;   (testing "Make sure we get a 200 response"
;;     (let [response (api/app-routes (request :get "/services/1"))]
;;       (is (= (:status response) 200)))))

;; (deftest add-service
;;   (testing "Make sure we have 2 services"
;;     (let [response (api/handler (-> (request :get "/services")
;;                                     (content-type "application/json")))
;;           n (count (cheshire/parse-string (:body response)))]
;;       (is (= (:status response) 200))
;;       (is (= n 2))))

;;   (testing "Try to add malformed service using JSON"
;;     (let [response (api/handler (-> (request :post "/services")
;;                                     (body "{")
;;                                     (content-type "application/json")))]
;;       (is (= (:status response) 400))))

;;   (testing "Try to add unprocessable service using JSON"
;;     (let [response (api/handler (-> (request :post "/services")
;;                                     (body (cheshire/generate-string
;;                                            {:nam "akvo-dash"}))
;;                                     (content-type "application/json")))]
;;       (is (= (:status response) 422))))

;;   (testing "Add new service using JSON"
;;     (let [response (api/handler (-> (request :post "/services")
;;                                     (body (cheshire/generate-string
;;                                            {:name "akvo-dash"}))
;;                                     (content-type "application/json")))]
;;       (is (= (:status response) 303))))

;;   (testing "Make sure we have 3 services"
;;     (let [response (api/handler (-> (request :get "/services")
;;                                     (content-type "application/json")))
;;           n (count (cheshire/parse-string (:body response)))]
;;       (is (= (:status response) 200))
;;       (is (= n 3)))))
