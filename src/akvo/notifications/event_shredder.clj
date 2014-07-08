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

(ns ^{:doc "Provides the pre-jobb of persisting service events & put them on a
  queue for process"}
  akvo.notifications.event-shredder
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]
   [taoensso.timbre :refer (info)]
   [langohr.core :as rmq]
   [cheshire.core :as cheshire]
   [clojure.pprint :refer (pprint)]
   [langohr.channel :as lch]
   [langohr.queue :as lq]
   [langohr.basic :as lb]
   [akvo.notifications.db :as db]
   [akvo.notifications.utils :as utils :refer (setup-handler)]))

(defn pub-event
  "We pass on the event to the queue. It's important to carry on
  appropriate meta data."
  [meta event]
  ;; (println "shredding new event on queue")
  (let [conn (rmq/connect)
        chan (lch/open conn)
        exchange ""
        queue "notif.event-queue"
        message (cheshire/generate-string event)]
    (pprint message)
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type (:content-type meta)
                :type (:type meta))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Component

(defrecord EventShredder [queue uri connection channel]
  Lifecycle

  (start [this]
    (if (nil? channel)
      (try
        (let [conn (rmq/connect {:uri uri})
              chan (lch/open conn)]
          (setup-handler chan conn queue
                         (fn [ch meta ^bytes payload]
                           (pub-event meta
                                      (db/create-event (:db this)
                                                       meta
                                                       (String. payload
                                                                "UTF-8")))))
          (assoc this :connection conn :channel chan))
        (catch Exception e (do
                             (println "Can't connect to RabbitMQ")
                             (println (.getMessage e))
                             (System/exit 0)))))

    (info "\n Starting event shredder")
    this)

  (stop [this]
    (info "\n Stopping event shredder")
    this))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn event-shredder
  [queue-host queue-port queue-user queue-password queue-vhost]
  (let [queue-name "notif.services-events"
        vhost      (utils/vhost queue-vhost)
        uri        (str "amqp://" queue-user ":" queue-password "@"
                        queue-host ":" queue-port vhost)]
    (map->EventShredder {:queue queue-name
                         :uri uri})))
