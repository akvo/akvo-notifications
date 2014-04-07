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
   [com.stuartsierra.component :refer (Lifecycle)]
   [clojure.pprint :refer (pprint)]
   [langohr.core :as rmq]
   [langohr.channel :as lch]
   [langohr.queue :as lq]
   [langohr.consumers :as lc]
   [langohr.basic :as lb]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Handler

(defn simple-handler
  [ch meta ^bytes payload]
  (println (format "[consumer] got message: %s" (String. payload "UTF-8"))))

(defn setup-handler
  [channel connection handler queue]
  (println "Setting up handler")
  (lq/declare channel queue :exclusive false :auto-delete false)
  (lc/subscribe channel queue handler :auto-ack false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Message Shredder component

(defrecord MessageShredder [queue connection channel ds]
  Lifecycle

  (start [this]
    (if (= channel nil)
      (let [conn (rmq/connect)
            chan (lch/open conn)]
        (println "; Turtles take care, spawning the message shredder")
        (setup-handler chan conn simple-handler queue)
        (pprint conn)
        (pprint chan)
        (assoc this :connection conn :channel chan))
      this))

  (stop [this]
    (when channel
      (println "; message shredder going down")
      (rmq/close channel)
      (rmq/close connection))
    (assoc this :connection nil :channel nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn new-shredder
  [queue]
  (map->MessageShredder {:queue queue}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; REPL play

;; (def ^{:const true} default-exchange-name "")

;; (def rabbit-formula
;;   ^{:doc "To be passed around"}
;;   (let [connection (rmq/connect)
;;         channel    (lch/open connection)
;;         queue      "akvo.service-events"]
;;     {:conn connection :ch channel :qname queue}))

;; (defn send-simple-message
;;   "Simple client example"
;;   [{:keys [ch qname]}]
;;   (lb/publish ch default-exchange-name qname "Haj from Clojure"
;;               :content-type "text/plain" :type "test.hi"))

                                        ; (send-simple-message rabbit-formula)
