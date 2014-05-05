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

(ns ^{:doc "Simple in memory datastore that uses fixture data"}
  akvo.notifications.datastore-mem
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [clojure.pprint :refer (pprint)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fixture data

(def services-data
  (atom [{:id    1
          :name  "akvo-rsr"
          :links [{:rel "self" :href "/services/1"}]}
         {:id    2
          :name  "akvo-flow"
          :links [{:rel "self" :href "/services/2"}]}]))

(def users-data
  (atom [{:id    1
          :name  "Bob"
          :links [{:rel "self"
                   :href "/users/1"}]}
         {:id    2
          :name  "Jane"
          :links [{:rel "self"
                   :href "/users/1"}]}]))

(def events-data
  (atom []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

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
;;; Memory datastore component

(defrecord DatastoreMem [host port data]
  Lifecycle

  (start [this]
    (println "; Starting memory datastore...")
    (assoc this :data data))

  (stop [this]
    (println "; Stopping memory datastore...")
    (assoc this :data {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn new-datastore [host port]
  (map->DatastoreMem {:services services-data
                      :users    users-data
                      :events   events-data
                      :host     host
                      :port     port}))

(defn port [datastore]
  (:port datastore))

(defn services-coll
  "Return all services"
  [datastore]
  {:pre [(:services datastore)]}
  @(:services datastore))

(defn new-service [old-services name]
  (let [id (inc (count old-services))]
    {:id id
     :name name
     :links [{:rel  "self"
              :href (format "/services/%s" id)}]}))

(defn add-service
  [datastore new-name]
  (let [old (:services datastore)
        new (swap! old conj (new-service @old new-name))]
    (count new)))

(defn service
  [datastore id]
  {:pre [(:services datastore)]}
  ((keyword id) (tuple-vec->id-tuple @(:services datastore))))

(defn users-coll
  "Returns all users"
  [datastore]
  {:pre [(:users datastore)]}
  @(:users datastore))

(defn user
  [datastore id]
  {:pre [(:users datastore)]}
  ((keyword id) (tuple-vec->id-tuple @(:users datastore))))

(defn new-event
  [datastore message-payload]
  (println (format "Should create new event from: [%s]" message-payload)))
