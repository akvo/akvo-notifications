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

(ns ^{:doc "Component that handles drafts (raw messages that carry state from
  services on the message queue. It's a queue before the actual queue
  which once a common data store exists should be cut."}
  akvo.notifications.drafts
  (:require [akvo.notifications.events :as e]
            [akvo.notifications.utils :refer (build-vhost setup-handler)]
            [clj-time.coerce :as c]
            [cheshire.core :as cheshire]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :refer (Lifecycle)]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]
            [schema.core :as s]
            [taoensso.timbre :refer (debug warn)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Publish drafts on message queue

(defn prepare-draft
  "Creates an outline for an event which is neither persited or have an
  id. We set id to be nil to be able to validate the event with a schema
  before it's persisted and got it's real id."
  [{:keys [:timestamp :type]} body]
  {:body body
   :id nil
   :timestamp (c/to-long timestamp)
   :type type})

(defn validate-draft
  "Validates drafts based on event schemas. If we can't "
  [schemas meta draft]
  (try
    (s/validate ((keyword (:type meta)) schemas) draft)
    (catch Exception e (warn (.getMessage e) draft))))

(defn draft->event
  "Dispatch to configured data store and call create-event. "
  [store event]
  ((resolve (symbol (:backend store) "create-event")) (:data store) event))

(defn publish-event
  "Put the event on the queue to be processed. Also make sure we carry
  relevant meta data. At the moment we do only send events encoded as
  application/json."
  [event]
  (let [conn (rmq/connect)
        chan (lch/open conn)
        exchange ""
        queue "notif.events"
        message (cheshire/generate-string {:id (:id event)})]
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type "application/json"
                :type (:type event))))

(defn handle-draft
  "Threads draft to a persisted event with id on the event queue - ready
  to be processed."
  [store meta message]
  (some->> (e/parse-message meta message)
           (prepare-draft meta)
           (validate-draft e/schemas meta)
           (draft->event store)
           (publish-event)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Draft component

(defrecord Drafts [queue uri connection channel]
  Lifecycle

  (start [this]
    (if (nil? channel)
      (try
        (let [conn (rmq/connect {:uri uri})
              chan (lch/open conn)]
          (setup-handler chan conn queue
                         (fn [ch meta ^bytes payload]
                           (handle-draft (:store this) meta
                                         (String. payload "UTF-8"))))
          (assoc this :connection conn :channel chan))
        (catch Exception e (do
                             (warn "Can't connect to RabbitMQ")
                             (warn (.getMessage e))
                             (comment (System/exit 0))))))
    (debug "Drafts component started")
    this)

  (stop [this]
    (debug "Drafts component stopped")
    this))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn drafts
  [host port user password vhost]
  (map->Drafts {:queue "notif.drafts"
                :uri   (str "amqp://" user ":" password "@" host ":" port
                            (build-vhost vhost))}))
