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

(ns ^{:doc "Chews on the queue of events"}
  akvo.notifications.event-handler
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [clojure.pprint :refer (pprint)]
   [langohr.basic :as lb]
   [langohr.channel :as lch]
   [langohr.core :as rmq]
   [langohr.queue :as lq]
   [taoensso.timbre :refer (info)]
   [akvo.notifications.db :as db]
   [akvo.notifications.utils :refer (read-message setup-handler vhost)]))

(defn get-user-id
  [identifier] ; email?
  identifier)

(defmulti handle-event
  "Dispatch event processing by type. Using the AMQP meta field intead
  of an internal type will make it easier to migrate to a stricter
  serialization format if wanted."
  (fn [db meta message] (:type meta)) :default :unsupported)

(defmethod handle-event :unsupported [db meta message]
  (info "\nGot unsuported message type\n" meta "\n" message))

(defmethod handle-event "notif.start-subscription"
  [db meta message]
  (info "Handle notif.start-subscription")
  (let [event   (read-message meta message)
        service (:service event)
        item    (:item event)
        user    (get-user-id (:user event))]
    (db/start-subscription db service item user)))

(defmethod handle-event "notif.end-subscription"
  [db meta message]
  (info "Handle notif.end-subscription")
  (let [event   (read-message meta message)
        service (:service event)
        item    (:item event)
        user    (get-user-id (:user event))]
    (db/end-subscription db service item user)))

(defmethod handle-event "notif.project-donation"
  [db meta message]
  (info "Handle notif.project-donation")
  (let [event    (read-message meta message)
        service  (:service event)
        item     (:item event)
        amount   (:amount event)
        currency (:currency event)]
    (db/project-donate db service item amount currency)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Component

(defrecord EventHandler [queue uri connection channel]
  Lifecycle

  (start [this]
    (if (nil? channel)
      (try
        (let [conn (rmq/connect {:uri uri})
              chan (lch/open conn)]
          (setup-handler chan conn queue
                         (fn [ch meta ^bytes payload]
                           (try
                             (handle-event (:db this)
                                           meta
                                           (String. payload "UTF-8"))
                             (catch AssertionError e (info "Got error: " e)))))
        (assoc this :connection conn :channel chan))
        (catch Exception e (do
                             (println "Can't connect to RabbitMQ")
                             (println (.getMessage e))
                             (System/exit 0)))))

    (info "\n Starting event handler")
    this)

  (stop [this]
    (info "\n Stopping event handler")
    this))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn event-handler
  [queue-host queue-port queue-user queue-password queue-vhost]
  (let [queue-name "notif.event-queue"
        uri (str "amqp://" queue-user ":" queue-password "@"
                 queue-host ":" queue-port (vhost queue-vhost))]
    (map->EventHandler {:queue queue-name
                        :uri uri})))
