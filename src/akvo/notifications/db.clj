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

(ns ^{:doc "Data abstraction layer, used to provide a common API
for the "}
  akvo.notifications.db
  (:require
   [akvo.notifications.utils :refer (read-message)]
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :refer (Lifecycle)]
   [taoensso.timbre :refer (info)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Component

(defrecord DB []
  Lifecycle

  (start [this]
    (info "\n Starting data")
    this)

  (stop [this]
    (info "\n Stopping data")
    this))

(defn db [backend]
  (map->DB {:backend backend}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Subscription

(defn start-subscription
  [db service item who]
  (println "start subscription..."))

(defn end-subscription
  [db service item who]
  (println "end subscription..."))

(defn service-action
  [db service item what]
  (println "service action..."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events

(defn create-event
  "We don't validate messages at this point, just pass the message body
  in an event."
  [db meta message-body]
  (let [message (read-message meta message-body)
        fn      (symbol (:backend db) "add-event")]
    ((resolve fn) (:store db) message)))

(defn events-coll
  "Returns the events collection."
  [db]
  (let [fn (symbol (:backend db) "events-coll")]
    ((resolve fn) (:store db))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services

(defn- existing-services
  "Returns existing services as an id indexed map."
  [coll]
  (reduce #(assoc %1 (:name %2) (:id %2)) {} coll))

(defn services-coll
  "Returns the services collection."
  [db]
  (let [fn (symbol (:backend db) "services-coll")]
    ((resolve fn) (:store db))))

(defn create-service
  "Creates a new service."
  [db new-name]
  (let [fn (symbol (:backend db) "create-service")]
    ((resolve fn) (:store db) new-name)))

(defn service
  "Returns an individual service."
  [db id]
  (let [fn (symbol (:backend db) "service")]
    ((resolve fn) (:store db) id)))

(defn service-exists?
  "Returns true if a service with the provided service name exists."
  [db service-name]
  (let [fn (symbol (:backend db) "service-exists?")]
    ((resolve fn) (:store db) service-name)))

(defn valid-service-name?
  "Validates the provided service name against the defined rules."
  [service-name]
  (.startsWith service-name "akvo-"))
