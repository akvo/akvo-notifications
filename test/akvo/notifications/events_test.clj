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

(ns akvo.notifications.events-test
  (:require [akvo.notifications.events :as e]
            [clojure.test :refer :all]
            [schema.core :as s]))

(defn validate-event
  "Helper function"
  [schemas event]
  (s/validate ((keyword (:type event)) schemas) event))

(deftest schema-test

  (testing "start-subscription-valid"
    (let [valid {:body      {:service "akvo-dash"
                             :item    "project-4"
                             :suid    4
                             :email   "bob@example.com"
                             :name    nil}
                 :id        nil
                 :timestamp 893462400000
                 :type      "start-subscription"}]
      (is (= (validate-event e/schemas valid) valid))))

  (testing "start-subscription-invalid"
    (is (thrown? Exception
                 (validate-event e/schemas
                                 {:body      {:service "akvo-dash"
                                              :item    "project-4"
                                              :suid    "5"
                                              :email   "test@example.se"}
                                  :id        nil
                                  :timestamp 893462400000
                                  :type      "start-subscription"}))))

  (testing "end-subscription-valid"
    (let [valid {:body      {:service "akvo-rsr"
                             :item    "project-3"
                             :suid    2
                             :email   "test@example.org"
                             :name    nil}
                 :id        1
                 :timestamp 893462400000
                 :type      "end-subscription"}]
      (is (= (validate-event e/schemas valid) valid))))

  (testing "end-subscription-invalid"
    (is (thrown? Exception
                 (validate-event e/schemas
                                 {:body      {:service "akvo-dash"
                                              :item    "project-4"
                                              :suid    42
                                              :email   3}
                                  :id        3
                                  :timestamp "Not valid date"
                                  :type      "end-subscription"})))))
