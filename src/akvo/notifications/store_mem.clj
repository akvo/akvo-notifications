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

(ns ^{:doc "Memory storage component."}
  akvo.notifications.store-mem
  (:require [clojure.pprint :refer (pprint)]
            [clojure.set :refer (union)]
            [clojure.string :refer (blank?)]
            [com.stuartsierra.component :refer (Lifecycle)]
            [taoensso.timbre :refer (debug)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Data

(def store-data
  {:events         (atom [])

   :services       (atom {})

   :subscriptions  (atom {})

   :services-users (atom {})

   :users          (atom {})})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utilities

(defn tuple-vec->id-tuple
  "\"Vector of truples to tuples of id's\"; transforms a vector of
   tuples into a map with the tuples id as keywords. Example: [{:id 1
   :name \"Bob} {:id 2 :name \"Jane\"}] -> {:1 {:id :name \"example\"} :2
   {:id 2 :name \"Jane }}."
  [tuple-vec]
  {:pre [(vector? tuple-vec)]}
  (if (empty? tuple-vec)
    {}
    (reduce #(assoc %1 (keyword (str (:id %2))) %2) {} tuple-vec)
    ))

(defn tuple-vec->email-tuple
  "\"Vector of truples to tuples of id's\"; transforms a vector of
   tuples into a map with the tuples id as keywords. Example: [{:id 1
   :name \"Bob} {:id 2 :name \"Jane\"}] -> {:1 {:id :name \"example\"} :2
   {:id 2 :name \"Jane }}."
  [tuple-vec]
  {:pre [(vector? tuple-vec)]}
  (if (empty? tuple-vec)
    {}
    (reduce #(assoc %1 (keyword (str (:email %2))) %2) {} tuple-vec)))

(defn tuple-vec->keyword-tuple
  "\"Vector of truples to tuples of id's\"; transforms a vector of
   tuples into a map with the tuples id as keywords. Example: [{:id 1
   :name \"Bob} {:id 2 :name \"Jane\"}] -> {:1 {:id :name \"example\"} :2
   {:id 2 :name \"Jane }}."
  [tuple-vec index-key]
  {:pre [(vector? tuple-vec)
         ()]}
  (if (empty? tuple-vec)
    {}
    (reduce #(assoc %1 (keyword (str (index-key %2))) %2) {} tuple-vec)))

(defn coll-nth
  "From a map with id."
  [coll id]
  (second (first (filter (fn [[k v]] (= id (:id v)))
                         coll))))

(defn str-map->id-map
  "Convert a string indexed map to an id indexed map."
  [coll]
  {:pre [(map? coll)]}
  (reduce (fn [new-coll [k v]] (assoc new-coll (:id v) v))
          {} coll))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Component

(defrecord MemStorage []
  Lifecycle

  (start [this]
    (debug "Starting memory store component")
    this)

  (stop [this]
    (debug "Stopping memory store component")
    this))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn store
  "Initialize the Memory store component."
  [backend]
  (map->MemStorage {:backend (str "akvo.notifications." backend)
                    :data    store-data}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event

(defn events-coll
  "Get events collection."
  [{:keys [:events] :as data}]
  {:pre [(map? data)]}
  @events)

(defn events-nth
  "Get nth event from events collection."
  [{:keys [:events] :as data} id]
  {:pre [(map? data)
         (number? id)]}
  (first (filter (fn [event] (= id (:id event))) @events)))

(defn- new-event
  "Returns a new event."
  [events event]
  (assoc event :id (inc (count events))))

(defn create-event
  "Adds the event to the events collection."
  [{:keys [:events]} event]
  (swap! events conj (new-event @events event))
  (last @events))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services

(defn services-coll
  "Gets the services collection."
  [data]
  (str-map->id-map @(:services data)))

(defn- service-exists?
  [services service-name]
  {:pre [(map? services)
         (string? service-name)]}
  (contains? services service-name))

(defn- new-service
  "Returns a new service."
  [services service-name]
  {:pre [(map? services)
         (string? service-name)]}
  (let [id (inc (count services))]
    {service-name {:id id
                   :name service-name
                   :subscribers {}
                   :link [{:rel "self"
                           :href (format "/services/%s" id)}]}}))

(defn- grab-service
  "Either create and return or return with existing service."
  [services service-name]
  {:pre [(map? services)
         (string? service-name)]}
  (let [service (get services service-name)]
    (if service
      {service-name service}
      (new-service services service-name))))

(defn service!
  "Provided with proper arguments will return new or existing service."
  [{:keys [:services] :as data} service-name]
  {:pre [(map? data)
         (string? service-name)
         (not (blank? service-name))]}
  (let [new-services (swap! services conj (grab-service @services
                                                          service-name))]
    (get new-services service-name)))

(defn services-nth
  "Returns the nth service from the services collection."
  [{:keys [:services] :as data} id]
  {:pre [(map? data)
         (number? id)]}
  (coll-nth @services id))

(defn get-subscribers
  "Get all subscripbers for a service."
  [data service-name item]
  (get-in @(:services data) [service-name :subscribers item]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users

(defn users-coll
  "Get the users collection."
  [data]
  (str-map->id-map @(:users data)))

(defn new-user
  "Returns a new user."
  [users service-name suid email name]
  {:pre [(map? users)
         (string? service-name)
         (number? suid)
         (string? email)
         (string? name)]}
  {email {:id       (inc (count users))
          :suid     {(keyword service-name) suid}
          :email    email
          :name     name
          :settings {:email false}
          :subscriptions {}
          :unread   #{}}})

(defn grab-user
  "Either return with existing user or create and return new user."
  [users service-name suid email name]
  {:pre [(map? users)
         (string? service-name)
         (number? suid)
         (string? email)
         (string? name)]}
  (let [user (get users email)]
    (if user
      {email user}
      (new-user users service-name suid email name))))

(defn user!
  "Get or creates a user."
  [{:keys [:servcies :users] :as data} service-name suid email name]
    {:pre [(map? data)
         (string? service-name)
         (number? suid)
         (string? email)
         (string? name)]}
    (let [service (service! data service-name)]
      (get (swap! users conj (grab-user @users (:name service) suid email
                                        name))
           email)))

(defn users-nth
  "Return nth user from the user collection."
  [{:keys [:users] :as data} id]
  {:pre [(map? data)
         (number? id)]}
  (coll-nth @users id))

(defn add-notification
  "Adds an event to users unread notifications."
  [users email event-id]
  {:pre [(string? email)
         (number? event-id)]}
  (swap! users assoc-in [email :unread]
         (union (get-in @users [email :unread])
                #{event-id})))

(defn user-notifed
  "Remove event from users unread notifications."
  [{:keys [:users] :as data} email events]
  {:pre [(map? data)
         (string? email)
         (vector? events)]}
  (swap! users assoc-in [email :unread]
         (apply disj (get-in @users [email :unread] #{}) events))
  (debug "Set events: " events " as read for user: " email))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Subscriptions

;;; Start

(defn subscribe-user-to-item
  "Subscribe user to item."
  [services service-name item uid]
  (swap! services assoc-in [service-name :subscribers item]
         (union (get-in @services [service-name :subscribers item])
                #{uid})))

(defn subscribe-item-to-user
  "Subscribe item to user."
  [users service-name item email]
  (swap! users assoc-in [email :subscriptions service-name]
         (union (get-in @users [email :subscriptions service-name])
                #{item})))

(defn start-subscription
  "Handle the start of a subscription."
  [{:keys [:services :users] :as data} service-name item uid email name]
  {:pre [(map? data)
         (string? service-name)
         (string? item)
         (number? uid)
         (string? email)
         (string? name)]}
  (subscribe-user-to-item services service-name item uid)
  (subscribe-item-to-user users service-name item email)
  (debug uid " started subscription on " service-name item)
  {:subscription "Started"})

;;; End

(defn unsubscribe-user-from-item
  "Unsubscribe user from item."
  [services service-name item uid]
  (swap! services assoc-in [service-name :subscribers item]
         (disj (get-in @services [service-name :subscribers item] #{}) uid)))

(defn unsubscribe-item-from-user
  "Unsubscribe item from user."
  [users service-name item email]
  (swap! users assoc-in [email :subscriptions service-name]
         (disj (get-in @users [email :subscriptions service-name] #{}) item)))

(defn end-subscription
  "Handle the end of a subscription."
  [{:keys [:services :users] :as data} service-name item uid email]
  {:pre [(map? data)
         (string? service-name)
         (string? item)
         (number? uid)
         (string? email)]}
  (unsubscribe-user-from-item services service-name item uid)
  (unsubscribe-item-from-user users service-name item email)
  (debug uid " ended subscription on " service-name item)
  {:subscription "Ended"})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Service actions

(defn project-donation
  "When a project recieves a donation."
  [data service-name item amount currency event-id email]
  {:pre [(map? data)
         (string? service-name)
         (string? item)
         (number? amount)
         (string? currency)
         (number? event-id)]}
  (let [users (get-subscribers data service-name item)]
    (dorun (map #(add-notification (:users data)
                                   (:email (users-nth data %))
                                   event-id)
                users))))
