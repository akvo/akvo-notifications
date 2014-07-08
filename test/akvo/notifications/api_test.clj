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
  (:require
   [akvo.notifications.api :as api]
   [akvo.notifications.main :as main]
   [akvo.notifications.systems :as systems]
   [akvo.notifications.test-fixture :refer (system-fixture base-url)]
   [cheshire.core :as cheshire]
   [clj-http.client :as httpc]
   [clojure.edn :as edn]
   [clojure.test :refer :all]
   [clojure.tools.cli :refer (parse-opts)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Setup

(def services-url (str base-url "/services"))
(use-fixtures :once system-fixture)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Routes

(deftest root
  (testing "Supported media types"
    (let [url       base-url
          edn-resp  (httpc/get url {:accept "application/edn"})
          json-resp (httpc/get url {:accept "application/json"})]
      (is (= (:status edn-resp) 200))
      (is (= (:status edn-resp) 200))))

  (testing "Typical unsupported media types"
    (let [url        base-url
          xml-resp   (httpc/get url
                                {:accept "application/xml"
                                 :throw-exceptions false})
          html-resp  (httpc/get url
                                {:accept "text/html"
                                 :throw-exceptions false})
          plain-resp (httpc/get url
                                {:accept "text/plain"
                                 :throw-exceptions false})]
      (is (= (:status xml-resp) 406))
      (is (= (:status html-resp) 406))
      (is (= (:status plain-resp) 406))))

  (testing "API map"
    (let [url base-url
          resp (httpc/get url {:accept "application/edn"})]
      (is (= (edn/read-string (:body resp)) api/api-map)))))

(deftest not-found
  (testing "Not found route with valid media type"
    (let [url (str base-url "/non-existing")
          resp (httpc/get url
                          {:accept "application/edn"
                           :throw-exceptions false})]
      (is (= (:status resp) 404))))

  (testing "Not found route with non valid media type"
    (let [url (str base-url "/non-existing")
          resp (httpc/get url
                          {:accept "application/xml"
                           :throw-exceptions false})]
      (is (= (:status resp) 406)))))

(deftest services-coll

  (testing "Make sure we have 2 services (using json)"
    (let [url  services-url
          resp (httpc/get url {:accept "application/json"})
          n    (count (cheshire/parse-string (:body resp)))]
      (is (= (:status resp) 200))
      (is (= n 2))))

  (testing "Try to add malformed service (using json)"
    (let [url  services-url
          resp (httpc/post url
                           {:accept :json
                            :body "{"
                            :content-type :json
                            :throw-exceptions false})]
      (is (= (:status resp) 400))))


  (testing "Try to add unprocessable service (using json)"
    (let [url  services-url
          resp (httpc/post url
                           {:accept :json
                            :body (cheshire/generate-string {:nam "akvo-dash"})
                            :content-type :json
                            :throw-exceptions false})]
      (is (= (:status resp) 422))))

  (testing "Add new service (using json)"
    (let [url   services-url
          body  (cheshire/generate-string {:name "akvo-dash"})
          resp  (httpc/post url
                            {:accept       :json
                             :body         body
                             :content-type :json})
          trail (:trace-redirects resp)]
      (is (= (:status resp) 200))
      (is (= (first trail) services-url))
      (is (= (second trail) (str services-url "/3"))))))
