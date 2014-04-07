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

(ns akvo.notifications.datastore-mem-test
  (:require [akvo.notifications.datastore-mem :refer :all]
            [clojure.test :refer :all]))

(deftest tuple-vec->id-tuple-test
  (testing "Empty vector"
    (is (= (tuple-vec->id-tuple [])
           {})))
  (testing "Empty tuple"
    (is (= (tuple-vec->id-tuple {})
           {})))
  (testing "Vector with single tuple"
    (is (= (tuple-vec->id-tuple [{:id 1 :name "Bob"}])
           {:1 {:id 1 :name "Bob"}})))
  (testing "Vector with two tuples"
    (is (= (tuple-vec->id-tuple [{:id 1 :name "Bob"}
                                 {:id 2 :name "Jane"}])
           {:1 {:id 1 :name "Bob"}
            :2 {:id 2 :name "Jane"}}))))
