;  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
;
;  This file is part of Akvo Notifications.
;
;  Akvo Notifications is free software: you can redistribute it and modify it
;  under the terms of the GNU Affero General Public License (AGPL) as published
;  by the Free Software Foundation, either version 3 of the License or any later
;  version.
;
;  Akvo Notifications is distributed in the hope that it will be useful, but
;  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
;  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
;  included below for more details.
;
;  The full license text can also be seen at
;  <http://www.gnu.org/licenses/agpl.html>.

(ns akvo.notifications.event-handlers
  ^{:doc "Subscribe message handlers to RabbitMQ"}
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb]
            [akvo.notifications.data-store :as ds]))

(def ^{:const true} default-exchange-name "")

(def rabbit-formula
  ^{:doc "To be passed around"}
  (let [connection (rmq/connect)
        channel    (lch/open connection)
        queue      "akvo.example"]
    {:conn connection :ch channel :qname queue}))

(defn simple-handler
  "Simple handler, just for show. Here we should decode protobuffs,
  validate message and then make sure we perform the correct
  actions. Correct actions can include to persist to the data store on
  the right user and maybe send emails."
  [ch meta ^bytes payload]
  (println (format "[consumer] got message: %s" (String. payload "UTF-8"))))

(defn setup-handlers
  "Declare a new channels and subscribe handlers. We need to review how
  to deal with a non existing message queue"
  [{:keys [conn ch qname]}]
  (lq/declare ch qname :exclusive false :auto-delete false)
  (lc/subscribe ch qname simple-handler :auto-ack false))

(defn close-handlers
  "Close channel & connection"
  [{:keys [conn ch]}]
  (rmq/close ch)
  (rmq/close conn))

(defn send-simple-message
  "Simple client example"
  [{:keys [ch qname]}]
  (lb/publish ch default-exchange-name qname "Haj from Clojure"
              :content-type "text/plain" :type "test.hi"))

                                        ; (send-simple-message rabbit-formula)

(defn init []
  "Init handlers with the formula"
  (setup-handlers rabbit-formula))

(defn destroy []
  "Gets called from core/destroy on exit"
  (close-handlers rabbit-formula))
