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


(ns akvo.notifications.store-mem-test
  (:require [akvo.notifications.main :as main]
            [akvo.notifications.store-mem :as sm]
            [clojure.pprint :refer (pprint)]
            [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [clojure.data :refer (diff)]
            [clojure.tools.cli :refer (parse-opts)]
            [com.stuartsierra.component :refer (system-map)]))

(def service-templ-1 {:name "test-serbvice-1"})

(def user-templ-1 {:service-name "test-service-1"
                   :suid         5
                   :email        "bob@akvo.test"
                   :name         "Bob Water"})

(def subscription-templ-1 {:item "project-99"})

(defn system [{:keys [backend]}]
  (system-map
   :store (sm/store backend)))

(defn mem-store-fixture [f]
  (try
    (main/init system (:options (parse-opts [] main/options)))
    (main/start)
    (f)
    (finally (main/stop))))

(use-fixtures :once mem-store-fixture)

(deftest services

  (testing "Services collection"
    (let [data           (get-in akvo.notifications.main/system [:store :data])
          service-name-1 "new-service-1"
          service-name-2 "new-service-2"]
      (is (empty? (swap! (:services data) empty))
          "Empty services data")
      (is (= 0 (count (sm/services-coll data)))
          "Make sure services data is empty when calling services-coll")
      (is (= 1  (:id (sm/service! data service-name-1)))
          "Verify service 1's id")
      (is (= 1 (count (sm/services-coll data)))
          "Make sure services collection have one item")
      (is (= service-name-2 (:name (sm/service! data service-name-2)))
          "Verify service 2's name")
      (is (= service-name-1 (:name (sm/services-nth data 1)))
          "Verify service 1's name using services-nth")
      (is (= 2 (count (sm/services-coll data)))
          "Make sure service collection have two items")
      (is (= 2 (:id (sm/service! data service-name-2)))
          "Make sure creation of dupplicate return the existing service")
      (is (thrown? AssertionError (sm/service! data ""))
          "Make sure passing empty string as service-name blows up")
      (is (thrown? AssertionError (sm/service! data 2))
          "Make sure passing number as service-name blows up")
      (is (thrown? AssertionError (sm/service! data " "))
          "Make sure passing strings with spaces as service-name blows up"))))


(deftest subscriptions
  (testing "Start a subscription"
    (let [data        (get-in akvo.notifications.main/system [:store :data])
          init        (do (swap! (:services data) empty)
                          (swap! (:users data) empty))
          new-service (sm/service! data (:name service-templ-1))
          new-user         (sm/user! data
                                     (:service-name user-templ-1)
                                     (:suid user-templ-1)
                                     (:email user-templ-1)
                                     (:name user-templ-1))
          new-subscription (sm/start-subscription data
                                                  (:service-name user-templ-1)
                                                  (:item subscription-templ-1)
                                                  (:id new-user)
                                                  (:email user-templ-1)
                                                  (:name user-templ-1))
          ]
      (is (= 1 (:id new-service)))
      (is (= 1 (:id new-user)))

      (is (contains? (sm/get-subscribers data
                                         (:service-name user-templ-1)
                                         (:item subscription-templ-1))
                     (:id new-user))
          "Make sure service has new user as a subscriber")

      (is (= "Ended"
             (:subscription (sm/end-subscription data
                                                 (:service-name user-templ-1)
                                                 (:item subscription-templ-1)
                                                 (:id  new-user)
                                                 (:email user-templ-1)))))

      (is (not (contains? (sm/get-subscribers data
                                              (:service-name user-templ-1)
                                              (:item subscription-templ-1))
                          (:id new-user)))
          "Make sure new user is not subscribed to new service"))))


(deftest users

  (testing "Creating new user with helper function"
    (let [data          (get-in akvo.notifications.main/system [:store :data])
          users         {}
          service       "test-service"
          suid          1
          email         "charlie@example.com"
          name          "Charlie Hope"
          fake-user     {email {:id            (inc (count users))
                                :suid          {(keyword service) suid}
                                :email         email
                                :name          name
                                :subscriptions {}
                                :settings      {:email false}
                                :unread        #{}}}
          renagade-user (sm/new-user users service suid email name)
          renagade-diff (diff fake-user renagade-user)]
      (is (and (nil? (first renagade-diff))
               (nil? (second renagade-diff)))
          "Validate new user data structure")))

  (testing "Creating a new user with public user!"
    (let [data (get-in akvo.notifications.main/system [:store :data])
          n (count (sm/users-coll data))
          service-name "new-service-3"
          suid 5
          email "tom@example.com"
          name "Tom Hope"
          new-user (sm/user! data service-name suid email name)]
      (is (= (inc n) (:id new-user))
          "Verify id on new user")
      (is (= email (:email (sm/users-nth data (:id new-user))))
          "Verify email using users-nth")
      (is (= (inc n) (count (sm/users-coll data)))
          "Verify number of users using users-coll"))))
