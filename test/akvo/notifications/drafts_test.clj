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

(ns akvo.notifications.drafts-test
  (:require [akvo.notifications.drafts :as d]
            [akvo.notifications.events :as e]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.test :refer :all]))


(def draft-meta {:timestamp (c/to-long (t/now))
                 :type "start-subscription"})

(def draft-body {:service "akvo-rsr"
                 :item    "project-4"
                 :suid    4
                 :name    nil
                 :email   "bob@example.com"})

(deftest drafts-test

  (testing "prepare-draft"
    (let [meta {:timestamp (c/to-long (t/now))
                :type "start-subscription"}
          body {:service "akvo-rsr"
                :item    "project-4"
                :suid    4
                :name    nil
                :email   "bob@example.com"}]
      (is (contains? (d/prepare-draft meta body) :id))))

  (testing "valid validate-draft"
    (let [unvalidated (d/prepare-draft draft-meta draft-body)
          validated (d/validate-draft e/schemas draft-meta unvalidated)
          ]
      (is (= unvalidated validated))))

  (testing "invalid validate-draft"
    (let [invalid-draft {:body      {:invalid "stuff"}
                         :type      "start-subscription"
                         :timestamp 123}]
      (is (nil? (d/validate-draft e/schemas draft-meta invalid-draft))))))
