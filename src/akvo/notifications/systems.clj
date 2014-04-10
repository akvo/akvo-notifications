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
   [akvo.notifications.api :as api]
   [akvo.notifications.app :as app]
   [akvo.notifications.datastore-mem :as ds-mem]
   [akvo.notifications.message-shredder :as ms]
   [com.stuartsierra.component :refer (system-map using)]))

(defn dev-system
  [config-options]
  (let [{:keys [api-port ds-port ds-host ms-queue]} config-options]
    (system-map
     :ds (ds-mem/new-datastore ds-host ds-port)
     :ms (using (ms/new-shredder ms-queue)
                {:ds :ds})
     :api (using (api/new-api api-port)
                 {:ds :ds})
     :app (using (app/new-app)
                 {:api :api
                  :ms  :ms}))))
