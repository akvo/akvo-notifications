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

(ns akvo.notifications.systems
  (:require
   [akvo.notifications.app :refer (app)]
   [akvo.notifications.db :refer (db)]
   [akvo.notifications.event-handler :refer (event-handler)]
   [akvo.notifications.event-shredder :refer (event-shredder)]
   [akvo.notifications.store-mem :refer (store)]
   [akvo.notifications.web-app :refer (webapp)]
   [com.stuartsierra.component :refer (system-map using)]))

(defn dev-system
  [config-options]
  (let [{:keys [web-port
                data-host data-port data-user data-password
                queue-host queue-port queue-user queue-password queue-vhost
                queue-name
                ]} config-options]
    (system-map
     :db-store (store)
     :db (using (db "akvo.notifications.store-mem")
                {:store :db-store})
     :es (using (event-shredder queue-host
                                queue-port
                                queue-user
                                queue-password
                                queue-vhost)
                {:db :db})

     :event-handler (using (event-handler queue-host
                                          queue-port
                                          queue-user
                                          queue-password
                                          queue-vhost)
                           {:db :db})

     :web (using (webapp web-port)
                 {:db :db})
     :app (using (app)
                 {:es :es
                  :event-handler :event-handler}))))
