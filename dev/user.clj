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

(ns
  ^{:doc "From the README of Stuart Sierras Component framework
  (https://github.com/stuartsierra/component) with love (MIT licenced."}
  user
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [clojure.tools.cli :refer (parse-opts)]
   [com.stuartsierra.component :as component]
   [cheshire.core :as cheshire]
   [langohr.core :as rmq]
   [langohr.channel :as lch]
   [langohr.queue :as lq]
   [langohr.basic :as lb]
   [akvo.notifications.systems :refer (dev-system)]
   [akvo.notifications.main :as main]))


(defn pub-message
  [event message-type]
  (let [conn (rmq/connect)
        chan (lch/open conn)
        exchange ""
        queue "notif.services-events"
        message (cheshire/generate-string event)]
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type "application/json"
                :type message-type)))

;; (pub-message {:service "akvo-dash"
;;               :item    "project-3"
;;               :user    "4"
;;               :creaded "13 June"}
;;              "notif.start-subscription")

;; (pub-message {:service "akvo-dash"
;;               :item    "project-2"
;;               :user    "2"
;;               :creaded "13 June"}
;;              "notif.end-subscription")


;; (pub-message {:service  "akvo-rsr"
;;               :item     "project-2"
;;               :currency "eur"
;;               :amount   "123"
;;               :created  "13 June"}
;;              "notif.project-donation")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Life cycle

(defn init []
  (main/init dev-system (:options (parse-opts [] main/options))))

(defn start []
  (main/start))

(defn stop []
  (main/stop))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
