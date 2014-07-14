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

(ns akvo.notifications.web-test
  (:require [akvo.notifications.test-fixture :refer (base-url system-fixture)]
            [akvo.notifications.web :refer :all]
            [clj-http.client :as httpc]
            [clojure.test :refer :all]))

(use-fixtures :once system-fixture)

(deftest pings

  (testing "Root with JSON"
    (let [resp (httpc/get base-url {:accept "application/json"})]
      (is (= (:status resp) 200))))

  (testing "Root with EDN"
    (let [resp (httpc/get base-url {:accept "application/edn"})]
      (is (= (:status resp) 200))))

  (testing "Unsupported media types"
    (let [url      base-url
          xml-resp (httpc/get url {:accept           "application/xml"
                                   :throw-exceptions false})]
      (is (= (:status xml-resp) 406))))

  (testing "Not Found"
    (let [url  (str base-url "/not-valid-url")
          resp (httpc/get url {:accept           "application/json"
                               :throw-exceptions false})]
      (is (= (:status resp) 404))))

  (testing "/services"
    (let [url  (str base-url "/services")
          resp (httpc/get url {:accept "application/json"})]
      (is (= (:status resp) 200)))))
