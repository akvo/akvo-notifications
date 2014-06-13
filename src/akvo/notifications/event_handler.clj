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
   [akvo.notifications.utils :refer (setup-handler vhost)]))


(defmulti handle-event
  "Dispatch event processing by type. Using the AMQP meta field intead
  of an internal type will make it easier to migrate to a stricter
  serialization format if wanted."
  (fn [db meta event] (:type meta)) :default :unsupported)

(defmethod handle-event :unsupported [db meta event]
  (info "\nGot unsuported message type\n" meta "\n" event))

(defmethod handle-event "notif.start-subscription"
  [db meta event]
  (pprint event)
  (db/start-subscription db :akvo-rsr :project-66 :4))

(defmethod handle-event "notif.end-subscription"
  [db meta event]
  (println "end subscription"))

(defmethod handle-event "notif.service-action"
  [db meta event]
  (println "service action"))


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
                           (handle-event (:db this)
                                         meta
                                         (String. payload "UTF-8"))))
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
