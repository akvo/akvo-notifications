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

(ns ^{:doc "Describes systems that is managed via the component framework"}
  akvo.notifications.systems
  (:require [akvo.notifications.drafts :refer (drafts)]
            [akvo.notifications.events :refer (events)]
            [akvo.notifications.store-mem :refer (store)]
            [akvo.notifications.web :refer (web)]
            [com.stuartsierra.component :refer (Lifecycle system-map using)]
            [taoensso.timbre :refer (info)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application component

(defrecord App []
  Lifecycle

  (start [this]
    (info "Akvo notifications started")
    this)

  (stop [this]
    (info "Akvo notifications stopped")
    this))

(defn app
  "Creates a new app component"
  []
  (map->App {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Production system

(defn production-system
  [{:keys [mode web-port backend
           queue-host queue-port queue-user queue-password queue-vhost]}]
  (system-map
   :store (store backend)
   :drafts (using (drafts queue-host
                          queue-port
                          queue-user
                          queue-password
                          queue-vhost)
                  {:store :store})
   :events (using (events queue-host
                          queue-port
                          queue-user
                          queue-password
                          queue-vhost)
                  {:store :store})
   :web (using (web mode web-port)
               {:store :store})
   :app (using (app)
               {:web :web
                :drafts :drafts})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test system

(def test-system production-system)
