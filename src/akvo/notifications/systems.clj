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
