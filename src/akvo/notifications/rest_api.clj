;  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
  (:require [akvo.notifications.data-store :as data]
            [akvo.notifications.rest-utils :as utils :refer [malformed?
                                                             handle-malformed
                                                             standard-config
                                                             processable?]]
            [cheshire.core :as cheshire]
            [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [defresource by-method]]
            [liberator.dev :refer [wrap-trace]]
            [ring.util.request :refer [request-url]]))

(def api-map
  {:name        "akvo-notifications"
   :description "This API...."
   :supported-media-types utils/available-media-types
   :links       [{:rel  "self"
                  :href "/"}]
   :resources   [{:name        "services"
                  :description "Represents Akvo internal services"
                  :links       [{:rel  "self"
                                 :href "/services"}]}
                 {:name        "users"
                  :description "Represent Akvo end users"
                  :links       [{:rel  "self"
                                 :href "/users"}]}]})

;;; Resources
(defresource root
  standard-config
  :handle-ok api-map)

(defresource not-found
  standard-config
  :allowed-methods [:get :post]
  :post-to-missing? false
  :exists? false)

;; Notifications resources
(defresource notif-coll
  standard-config
  :exists? false)

(defresource notif
  [id]
  standard-config
  :exists? false)

;; Services resources
(defn- existing-services
  "Service validator helper function"
  [coll]
  (reduce #(assoc %1 (:name %2) (:id %2)) {} coll))

(defn- services-validator
  "Validator fuction for adding new services. First "
  [body]
  {:pre [(:name body)
         (not (contains? (existing-services @data/services-data)
                         (:name body)))]}
  true)

(defresource services-coll
  standard-config
  :allowed-methods [:get :post]
  :handle-ok (fn [ctx] (data/services-coll))
  :processable? (by-method {:get true
                            :post (fn [ctx]
                                    (processable? ctx services-validator))})
  :post! (fn [ctx]
           (let [service-name (:name (:request-body ctx))
                 service-id   (data/add-service service-name)]
             {::id (str service-id)}))
  :post-redirect? (fn [ctx]
                    {:location (format "%s/%s"
                                       (request-url (get-in ctx [:request]))
                                       (::id ctx))}))

(defresource service
  [id]
  standard-config
  :exists? (fn [ctx] (not (nil? (data/service id))))
  :handle-ok (fn [_] (data/service id)))

;; Users resources
(defresource users-coll
  standard-config
  :allowed-methods [:get]
  :handle-ok (fn [ctx] (data/users-coll)))

 (defresource user
   [id]
   standard-config
   :exists? (fn [ctx] (not (nil? (data/user id))))
   :handle-ok (fn [_] (data/user id)))

;;; Routes
(defroutes endpoints
  (ANY "/" [] root)
  (ANY "/notifications" notif-coll)
  (ANY "/notifications/:id" [id] (notif id))
  (ANY "/services" [] services-coll)
  (ANY "/services/:id" [id] (service id))
  (ANY "/users" [] users-coll)
  (ANY "/users/:id" [id] (user id))
  (ANY "*" [] not-found))

(def handler (-> endpoints
                 (wrap-trace :header :ui)))
