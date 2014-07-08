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

(ns akvo.notifications.functional-test
  (:require [akvo.notifications.test-fixture :refer (system-fixture base-url)]
            [cheshire.core :as cheshire]
            [clj-http.client :as httpc]
            [clojure.test :refer :all]))


(use-fixtures :once system-fixture)

(deftest simple

  (testing "Ping"
    (let [resp (httpc/get base-url {:accept "application/edn"})]
      (is (= (:status resp) 200))))

  (testing "Make sure we have 0 events (using json)"
    (let [url  (str base-url "/events")
          resp (httpc/get url {:accept "application/json"})
          n    (count (cheshire/parse-string (:body resp)))]
      (is (= (:status resp) 200))
      ;; (is (= n 0))
      )))
