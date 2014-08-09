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

(ns akvo.notiticactions.h2-mem-store
  (:require [clojure.test :refer :all])
  (:import [org.h2.mvstore MVStore MVStore$Builder]))


(deftest mem-store
  (testing "openMap"
    (let [store (->
                  (MVStore$Builder.)
                  (.fileName nil) ;; implies in-memory
                  (.open))]
      
      (let [users (.openMap store "users")]
        (is (empty? users) "Users is empty"))
      
      (let [users (.openMap store "users")
            _ (.put users :email "nobody@akvo.org")
            tx (.commit store)]
        (is (= 1 tx) "First transaction")
        (is (= 1 (count users)) "Number of users")
        (is (= "nobody@akvo.org" (.get users :email)) "We can use keywords"))

      (let [services (.openMap store "services")]

        (.put services :s1 "test-service-1")
        (.commit store)

        (.put services :s2 "test-service-2")
        (.commit store)

        (is (= 2 (count services)))

        (is (= "test-service-1" (.get services :s1)))
        (is (= "test-service-2" (.get services :s2)))

        (.remove services :s1)
        (.commit store)

        (is (= 1 (count services)))
        (is (nil? (.get services :s1)))

        (.put services :s1 "test-service-1.1")
        (.commit store)

        (is (= "test-service-1.1" (.get services :s1)))

        (is (= "test-service-1") (-> services
                                   (.openVersion 1) ;; versioning
                                   (.get :s1))))

      (let [subscriptions (.openMap store "subscriptions")
            srv-list ["service-1" "service-2" "service-3"]
            data [{:id 1
                   :name "x"
                   :value true}
                  {:id 2
                   :name "y"
                   :value false}]]

        (.put subscriptions "nobody@akvo.org" srv-list)
        (.commit store)

        (is (= srv-list (.get subscriptions "nobody@akvo.org")))

        (.put subscriptions :data data)
        (.commit store)

        (is (= data (.get subscriptions :data)))))))
