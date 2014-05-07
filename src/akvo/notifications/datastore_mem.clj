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
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require
   [cheshire.core :as cheshire]
   [clojure.edn :as edn]
   [clojure.pprint :refer (pprint)]
   [com.stuartsierra.component :refer (Lifecycle)]
   [taoensso.timbre :refer (info)]))

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
          :name  "Bob Example"
          :email "bob@akvo.dev"
          :links [{:rel "self"
                   :href "/users/1"}]}
         {:id    2
          :name  "Jane Example"
          :email "jane@akvo.dev"
          :links [{:rel "self"
                   :href "/users/1"}]}]))

(def events-data
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
;;; Memory datastore component

(defrecord DatastoreMem [host port data]
  Lifecycle

  (start [this]
    ;; (info "\n; Starting memory datastore..")
    (assoc this :data data))

  (stop [this]
    ;; (info "\n; Stopping memory datastore...")
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events

;; (defn- new-event
;;   [old-events event]
;;   (let [id (inc (count old-events))]
;;     {:id id
;;      :content event}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events

(defn events-coll
  "Returns all users"
  [datastore]
  {:pre [(:events datastore)]}
  @(:events datastore))

;; (defn create-event
;;   [datastore event]
;;   {:pre [(not (nil? datastore))
;;          (not (nil? event))]}
;;   (let [old (:events datastore)
;;         new (swap! old conj (new-event @old event))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services-

(defn- new-service
  "Private helper that creates a new service in a transaction"
  [old-services name]
  (let [id (inc (count old-services))]
    {:id    id
     :name  name
     :links [{:rel  "self"
              :href (format "/services/%s" id)}]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services

(defn list-services
  "Return the services collection"
  [datastore]
  {:pre [(:services datastore)]}
  @(:services datastore))

(defn service
  "Returns a single service"
  [datastore id]
  {:pre [(:services datastore)]}
  ((keyword id) (tuple-vec->id-tuple @(:services datastore))))

(defn create-service
  "Creates a new service and returns the new id"
  [datastore new-name]
  {:pre [(not (nil? datastore))
         (.startsWith new-name "akvo-")]}
  (let [old (:services datastore)
        new (swap! old conj (new-service @old new-name))]
    (count new)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users-

(defn- user-by-id [db id]
  {:pre [(:users db)
         (integer? id)]}
  ((keyword id) (tuple-vec->id-tuple @(:users db))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users

(defn users-coll
  "Returns all users"
  [datastore]
  {:pre [(:users datastore)]}
  @(:users datastore))

(defn user
  [datastore id]
  {:pre [(:users datastore)]}
  ((keyword id) (tuple-vec->id-tuple @(:users datastore))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events

(defmulti process-message
  "..."
  (fn [message meta] (:content-type meta))
  :default :unsupported)

(defmethod process-message :unsupported [message meta]
  (info "Got unprocessable message" meta message))

(defmethod process-message "application/json"
  [message meta]
  (try
    (cheshire/parse-string message true)
    (catch JsonParseException e (info "Could not parse message of type: "
                                      (:content-type meta)
                                      "\nMessage: " message))))

(defmethod process-message "application/edn"
  [message meta]
  (try
    (edn/read-string message)
    (catch RuntimeException e (info "Coudl not parse message of type: "
                                    (:content-type meta)
                                    "\nMessage: " message))))

(defn new-event
  [old-events message]
  {:pre [(not (nil? (:item message)))
         (not (nil? (:item-type message)))
         (not (nil? (:service message)))
         (not (nil? (:timestamp message)))
         (not (nil? (:type message)))
         (not (nil? (:user message)))]}
  {:id (inc (count old-events))
   :item (:item message)
   :item-type (:item-type message)
   :service (:service message)
   :timestamp (:timestamp message)
   :type (:type message)
   :user (:user message)})

(defn create-event
  [datastore message]
  (let [old-events (:events datastore)
        new-events (swap! old-events conj (new-event @old-events message))]
    (info "\nStored new event: " (last new-events))))

(defn handle-message
  [datastore message meta]
  {:pre [(not (nil? datastore))
         (not (nil? meta))
         (not (nil? message))]}
  (if-let [body (process-message meta message)]
    (create-event datastore body)))
