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
   [com.stuartsierra.component :refer (Lifecycle)]
   [taoensso.timbre :refer (info)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Memory data

(def events-data
  (atom []))

(def services-data
  (atom []))

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
  (map->MemStorage {:events   events-data
                    :services services-data}))


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
  (not (contains? (existing-services @(:services data))
                  service)))
