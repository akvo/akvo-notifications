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

(ns ^{:doc "The Message Shredder takes messages containing events from internal
  services and send of to the datastore."}
  akvo.notifications.message-shredder
  (:require
   [akvo.notifications.datastore-mem :as ds]
   [com.stuartsierra.component :refer (Lifecycle)]
   [clojure.string :refer (blank?)]
   [cheshire.core :as cheshire]
   [langohr.core :as rmq]
   [langohr.channel :as lch]
   [langohr.queue :as lq]
   [langohr.consumers :as lc]
   [langohr.basic :as lb]
   [taoensso.timbre :refer (info)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private API

(defn setup-handler
  [channel connection queue handler]
  {:pre [(not (nil? channel))
         (not (nil? connection))]}
  (lq/declare channel queue :exclusive false :auto-delete false)
  (lc/subscribe channel queue handler :auto-ack true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Message Shredder component

(defrecord MessageShredder [queue uri connection channel ds]
  Lifecycle

  (start [this]
    (if (nil? channel)
      (try
        (let [conn (rmq/connect {:uri uri})
              chan (lch/open conn)]
          ;; (info "; Turtles take care, spawning the message shredder")
          (setup-handler chan conn queue
                         (fn [ch meta ^bytes payload]
                           (ds/handle-message (:ds this)
                                              meta
                                              (String. payload "UTF-8"))))
          (assoc this :connection conn :channel chan))
        (catch Exception e (do
                             (println "Can't connect to RabbitMQ")
                             (println (.getMessage e))
                             (System/exit 0))))
      this))

  (stop [this]
    (when channel
      ;; (info "; message shredder going down")
      (rmq/close channel)
      (rmq/close connection))
    (assoc this :connection nil :channel nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn new-shredder
  "Creates a new message shredder.  The RabbitMQ vhost is seen as empty
  is empty or /."
  [queue-host queue-port queue-user queue-password queue-vhost queue-name]
  {:pre [(not (nil? queue-name))]}
  (let [vhost (cond
               (blank? queue-vhost) "/%2F"
               (= "/" queue-vhost) "/%2F"
               :else                queue-vhost)
        uri (str "amqp://" queue-user ":" queue-password "@"
                 queue-host ":" queue-port vhost)]
    (map->MessageShredder {:queue queue-name
                           :uri uri})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; REPL play

(defn pub-edn-message
  [raw-message]
  (let [conn (rmq/connect)
        chan (lch/open conn)
        exchange ""
        queue "notif.services-events"
        message (pr-str raw-message)]
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type "application/edn"
                :type "service.subscription")))

;; (pub-edn-message {:item "project-42"
;;                   :item-type :project
;;                   :service :akvo-rsr
;;                   :timestamp "25 May"
;;                   :type :start-subscription
;;                   :user "bob@akvo.dev"})

(defn pub-json-message
  [raw-message]
  (let [conn (rmq/connect)
        chan (lch/open conn)
        exchange ""
        queue "notif.services-events"
        message (cheshire/generate-string raw-message)]
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type "application/json"
                :type "notif.start-subscription"
                )))

(pub-json-message {:item "project-47"
                   :item-type :project
                   :service :akvo-rsr
                   :timestamp "12 June"
                   :created "11 June"
                   :user "bob@akvo.dev"})
