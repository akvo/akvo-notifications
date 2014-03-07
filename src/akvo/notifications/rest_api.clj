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

(ns akvo.notifications.rest-api
  ^{:doc "REST API for other services to consume."}
  (:require [liberator.core :refer [defresource resource]]
            [compojure.core :refer [defroutes ANY]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [akvo.notifications.data-store :as data]))

(def available-media-types
  ["text/plain" "text/csv" "application/json"
   "application/clojure;q=0.9"])

(def standard-config
  {:available-media-types available-media-types
   :handle-not-acceptable (format "We only speak: %s" available-media-types)})

(defresource index
  standard-config
  :handle-ok "OK")

(defresource services
  standard-config
  :handle-ok (fn [_] (data/services)))

(defresource services-details [service]
  standard-config
  :handle-ok (fn [_] (data/services-details service)))

(defroutes api
  (ANY "/" [] index)
  (ANY "/services/" [] services)
  (ANY "/services/:service/" [service] (services-details service))
  (route/not-found "Page not found"))

(def handler (wrap-params api))
