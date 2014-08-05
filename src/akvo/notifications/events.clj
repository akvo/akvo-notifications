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

(ns ^{:doc "Component that handle events. The namespace listens to a AMQP queue
  and then validates messages against matching event schemas and if
  valid compute relevant graph. Since both external and internal user
  id's exists 'suid' (service user id) is used for external user id's
  and uid for the internal user id."}
  akvo.notifications.events
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require [akvo.notifications.utils :refer (build-vhost setup-handler)]
            [cheshire.core :as cheshire :refer (parse-string)]
            [clojure.edn :as edn]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :refer (Lifecycle)]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [plumbing.core :refer (defnk fnk)]
            [plumbing.graph :as graph]
            [schema.core :as s]
            [taoensso.timbre :refer (debug info warn)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schema utilities

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn valid-service-name?
  "Verifies that the service name starts with akvo-<identifier> where
  identifier have to be at least one character long"
  [x]
  (and (.startsWith ^String x "akvo-")
       (> (count x) 5 )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def BaseEventSchema
  "Root event schema."
  {:id (s/maybe s/Int)
   :timestamp long
   :type s/Str})

(def BaseServiceEventSchema
  "BaseEventSchema + body with service."
  (deep-merge
   BaseEventSchema
   {:body
    {:service (s/both s/Str
                      (s/pred valid-service-name? 'valid-service-name?))}}))

(def SubscriptionsSchema
  "Validates start / end subscriptions messages."
  (deep-merge BaseServiceEventSchema
              {:body {:email s/Str
                      :item  s/Str
                      :name  (s/maybe s/Str)
                      :suid  s/Int}}))

(def ProjectDonationSchema
  "Validates project donations messages."
  (deep-merge BaseServiceEventSchema
              {:body {:amount   s/Int
                      :currency s/Str
                      :item     s/Str}}))

(def UserNotifiedSchema
  "Validates message recieved when a user got notified."
  (deep-merge BaseEventSchema
              {:body {:email s/Str
                      :events [s/Int]}}))

(def schemas
  "A map of all different schemas indexed by the event (keywordized) types."
  {:start-subscription SubscriptionsSchema
   :end-subscription   SubscriptionsSchema
   :project-donation   ProjectDonationSchema
   :user-notified      UserNotifiedSchema})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Graph utilities

(defmulti parse-message
  "Parses a message in JSON/ EDN and return a Clojure data structure."
  (fn [meta message] (:content-type meta))
  :default :unsupported)

(defmethod parse-message :unsupported [meta message]
  (warn "Got unprocessable message" meta message))

(defmethod parse-message "application/json"
  [meta message]
  (try
    (cheshire/parse-string message true)
    (catch JsonParseException e (warn "Could not parse message of type: "
                                      (:content-type meta)
                                      "\nMessage: " message))))

(defmethod parse-message "application/edn"
  [meta message]
  (try
    (edn/read-string message)
    (catch RuntimeException e (warn "Could not parse message of type: "
                                    (:content-type meta)
                                    "\nMessage: " message))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Graph units

(defnk get-event-id
  "First calls parse-message that will parse the message based on
  content type. Then pulls out the :id."
  [meta message]
  (:id (parse-message meta message)))

(defnk get-event
  "Based on the event id get the event from the configured data store."
  [store event-id]
  ((resolve (symbol (:backend store) "events-nth")) (:data store) event-id))

(defnk validate-event
  "Validate event with the corresponding schema."
  [event]
  (try
    (s/validate ((keyword (:type event)) schemas) event)
    (catch Exception e (do (warn (.getMessage e) event)
                           nil))))

(defnk get-or-create-user
  "Provide user either by getting existing or creating a new."
  [store validated]
  (debug "get-or-create-user")
  (let [email        (get-in validated [:body :user-email])
        service-name (get-in validated [:body :service])
        suid         (get-in validated [:body :suid])
        email        (get-in validated [:body :email])
        name         (get-in validated [:body :name])
        fn           (symbol (:backend store) "user!")]
    ((resolve fn) (:data store) service-name suid email name)))

(defnk start-subscription
  "With validated event and a user start a subscription at configured
  data store."
  [store validated-event user]
  (debug "start-subscription")
  (let [service-name (get-in validated-event [:body :service])
        item         (get-in validated-event [:body :item])
        fn           (symbol (:backend store) "start-subscription")]
    ((resolve fn) (:data store) service-name item (:id user) (:email user)
     (:name user))))

(defnk end-subscription
  "With validated event and a user stop a subscription at configured
  data store."
  [store validated user]
  (debug "end-subscription")
  (let [service (get-in validated [:body :service])
        item    (get-in validated [:body :item])
        fn      (symbol (:backend store) "end-subscription")]
    ((resolve fn) (:data store) service item (:id user) (:email user))))

(defnk project-donation
  "With configured data store make a project donation."
  [store validated]
  (debug "project-donation")
  (let [service-name (get-in validated [:body :service])
        item         (get-in validated [:body :item])
        amount       (get-in validated [:body :amount])
        currency     (get-in validated [:body :currency])
        event-id     (get-in validated [:id])
        email        (get-in validated [:body :email])
        fn           (symbol (:backend store) "project-donation")]
    ((resolve fn) (:data store) service-name item amount currency event-id
     email)))

(defnk user-notified
  "With configured data store remove event from users unread notifications."
  [store validated]
  (let [email  (get-in validated [:body :email])
        events (get-in validated [:body :events])
        fn     (symbol (:backend store) "user-notifed")]
    ((resolve fn) (:data store) email events)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Graphs

(def base-graph
  "From raw message provide a validated event."
  {:event-id get-event-id
   :event get-event
   :validated validate-event})

(def start-subscription-graph
  "Computed when user subscribes to an item."
  (assoc base-graph
    :user get-or-create-user
    :final start-subscription))

(def end-subscription-graph
  "Computed when user unsubscribes from an item."
  (assoc base-graph
    :user get-or-create-user
    :final end-subscription))

(def project-donation-graph
  "Computed on a project donation."
  (assoc base-graph
    :final project-donation))

(def user-notified-graph
  "Computed when user have been notifed."
  (assoc base-graph
    :final user-notified))

(def graphs
  "All the graphs in a map ready to be computed."
  {:start-subscription start-subscription-graph
   :end-subscription   end-subscription-graph
   :project-donation   project-donation-graph
   :user-notified      user-notified-graph})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Init graph computations

(defn handle-event
  "Init a graph computation to handle message of different kinds of
  types."
  [store meta message]
  ((graph/compile ((keyword (:type meta))
                   graphs))
   {:message message
    :meta    meta
    :store   store}))

;; (defn handle-event
;;   "Init a graph computation to handle message of different kinds of
;;   types."
;;   [store meta message]
;;   (try
;;     ((graph/compile ((keyword (:type meta))
;;                      graphs))
;;      {:message message
;;       :meta    meta
;;       :store   store})
;;     (catch Exception e (do
;;                          (warn "Could not handle event:" (.getMessage e))
;;                          {:handle-event-error (str "Could not handle event: "
;;                                                    (.getMessage e))}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events component

(defrecord Events [queue uri connection channel]
  Lifecycle

  (start [this]
    (if (nil? channel)
      (try
        (let [conn (rmq/connect {:uri uri})
              chan (lch/open conn)]
          (setup-handler chan conn queue
                         (fn [ch meta ^bytes payload]
                           (handle-event (:store this) meta
                                         (String. payload "UTF-8"))))
          (assoc this :connection conn :channel chan))
        (catch Exception e (do
                             (warn "Can't connect to RabbitMQ")
                             (warn (.getMessage e))
                             (comment (System/exit 0))))))
    (debug "Events component started")
    this)

  (stop [this]
    (debug "Events component stopped")
    this))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn events
  "Starts the Events component."
  [host port user password vhost]
  (map->Events {:queue "notif.events"
                :uri   (str "amqp://" user ":" password "@" host ":" port
                            (build-vhost vhost))}))
