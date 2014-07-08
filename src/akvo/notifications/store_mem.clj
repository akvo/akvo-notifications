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

(ns ^{:doc "Memory storage backend"}
  akvo.notifications.store-mem
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.set :refer (union)]
   [com.stuartsierra.component :refer (Lifecycle)]
   [taoensso.timbre :refer (info)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Memory data

(def events-data
  (atom []))

(def services-data
  (atom []))

(def subscriptions-data
  (ref {:services {}
        :subscribers {}}))

(def users-data
  (atom [{:id           1
          :qualified-id :akvo-dash-23
          :name         "Jane Example"
          :email        "jane@kardans.com"
          :service      :akvo-dash
          :settings     {:email false}}
         {:id           2
          :qualified-id :akvo-rsr-4
          :name         "Bob Example"
          :email        "bob@kardans.com"
          :service      :akvo-rsr
          :settings     {:email false}}]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utility

(defn tuple-vec->id-tuple
  "\"Vector of truples to tuples of id's\"; transforms a vector of
   tuples into a map with the tuples id as keywords. Example: [{:id 1
   :name \"Bob} {:id 2 :name \"Jane\"}] -> {:1 {:id :name \"example\"} :2
   {:id 2 :name \"Jane }}"
  [tuple-vec]
  (if (empty? tuple-vec)
    {}
    (reduce #(assoc %1 (keyword (str (:id %2))) %2) {} tuple-vec)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Component

(defrecord MemStorage []
  Lifecycle

  (start [this]
    (info "\n Starting memory storage")
    this)

  (stop [this]
    (info "\n Stopping memory storage")
    this))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn store []
  (map->MemStorage {:events        events-data
                    :services      services-data
                    :subscriptions subscriptions-data
                    :users         users-data}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events

(defn create-event
  [old-events event]
  (assoc event :id (inc (count old-events))))

(defn add-event
  [data event]
  (last (swap! (:events data)
               conj
               (create-event @(:events data) event))))

(defn events-coll
  [data]
  @(:events data))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users

(defn users-coll
  "Returns all users"
  [data]
  @(:users data))

(defn user
  [data id]
  ((keyword id) (tuple-vec->id-tuple @(:users data))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services

(defn- new-service
  "Private helper that creates a new service in a transaction"
  [old-services name]
  (let [id (inc (count old-services))]
    {:id    id
     :name  name
     :links [{:rel  "self"
              :href (format "/services/%s" id)}]}))

(defn services-coll
  [data]
  @(:services data))

(defn create-service
  [data name]
  (let [old (:services data)
        new (swap! old conj (new-service @old name))]
    (count new)))

(defn service
  [data id]
  ((keyword id) (tuple-vec->id-tuple @(:services data))))

(defn- existing-services
  [coll]
  (reduce #(assoc %1 (:name %2) (:id %2)) {} coll))

(defn service-exists?
  [data service]
  (contains? (existing-services @(:services data))
             service))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Subscriptions

;;; Start

(defn subscribe-user-to-item
  "Adds an user to an item"
  [subscriptions service item who]
  (alter subscriptions assoc-in [:services service item]
         (union (get-in @subscriptions [:services service item]) #{who})))

(defn subscribe-item-to-user
  "Adds an item to a user"
  [subscriptions service item who]
  (alter subscriptions assoc-in [:subscribers who service]
         (union (get-in @subscriptions [:subscribers who service]) #{item})))

(defn start-subscription
  "Subscriptions consists of two data structures. Service/item->who
  who->service/item They are updated in a transaction."
  [data service item who]
  {:pre [(service-exists? data (name service))]}
  (println "\nstart-subscription")
  (let [s (keyword service)
        i (keyword item)
        w (keyword who)]
    (dosync
     (subscribe-user-to-item (:subscriptions data) s i w)
     (subscribe-item-to-user (:subscriptions data) s i w)))
  (pprint (:subscriptions data))
  )

;;; End

(defn unsubscribe-user-from-item
  "Removes an user from an item"
  [subscriptions service item who]
  (alter subscriptions assoc-in [:services service item]
         (disj (get-in @subscriptions [:services service item]) who)))

(defn unsubscribe-item-from-user
  "Removes an item from an user"
  [subscriptions service item who]
  (alter subscriptions assoc-in [:subscribers who service]
         (disj (get-in @subscriptions [:subscribers who service]) item)))

(defn end-subscription
  ""
  [data service item who]
  (println "\nend-subscription")
  (let [s (keyword service)
        i (keyword item)
        w (keyword who)]
    (dosync
     (unsubscribe-user-from-item (:subscriptions data) s i w)
     (unsubscribe-item-from-user (:subscriptions data) s i w))))

(defn project-donate
  "With the help of the subscriptions data create notifications for
  users. "
  [data service-name item amount currency]
  ;; Review how to do service exists verifications.
  (println "Got donation"))
