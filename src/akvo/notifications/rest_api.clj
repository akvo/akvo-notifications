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
            [cheshire.core :as cheshire]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [compojure.core :refer [defroutes ANY]]
            [compojure.route :as route]
            [liberator.core :refer [defresource resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.request :refer [request-url]]))

;;; Setup
(def available-media-types
  ["application/json" "application/edn"])

(def error-responses
  {:404 {:reason "Resource not found."}
   :415 (format "We only speak: %s." available-media-types)
   :422 {:reason "Could not process request."}})

(def standard-config
  {:available-media-types       available-media-types
   :handle-not-acceptable       (:415 error-responses)
   :handle-not-found            (:404 error-responses)
   :handle-unprocessable-entity (:422 error-responses)})

(def api-map
  {:name        "akvo-notifications"
   :description "This API...."
   :supported-media-types available-media-types
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

;;; Helpers
(defn- slurp-body
  [ctx]
  (slurp (get-in ctx [:request :body])))

(defmulti #^{:private true} processable-body?
  "Dispatches to process body based on content-type"
  (fn [ctx] (get-in ctx [:request :content-type]))
  :default :unsupported)

(defmethod processable-body? :unsupported [_] false)
(defmethod processable-body? "application/json"
  [ctx]
  (cheshire/parse-string (slurp-body ctx) true))
(defmethod processable-body? "application/edn"
  [ctx]
  (edn/read-string (slurp-body ctx)))

(defmulti #^{:private true} processable?
  "Dispatch based on reqeust method. This since we only want to process
  the body for post requests."
  (fn [ctx validator] (get-in ctx [:request :request-method]))
  :default :unsupported)

(defmethod processable? :unsupported [_ _] false)
(defmethod processable? :get [_ _] true)
(defmethod processable? :post
  [ctx validator]
  (try
    (if-let [body (->> (processable-body? ctx)
                       validator
                       :name)]
      {:request-body body} false)
    (catch AssertionError e false)))

;;; Validators
(defn service-validator
  "This is a validator that works with the fixture data so we need to
  rework this function once we hook in a proper data store.

  First we verify that there is a \"name\" keyval. Second we make sure
  the name is not already taken."
  [data]
  {:pre [(:name data)
         (not (contains? (->> @data/services-data
                              (map #(assoc {} (:name %) (:id %)))
                              (into {}))
                         (:name data)))]}
  data)

;;; Resources
(defresource root
  standard-config
  :handle-ok api-map)

(defresource notif-coll
  standard-config
  :exists? false)

 (defresource notif
   [id]
   standard-config
   :exists? false)

(defresource services-coll
  standard-config
  :allowed-methods [:get :post]
  :handle-ok (fn [ctx] (data/services-coll))
  :processable? (fn [ctx] (processable? ctx service-validator))
  :post! (fn [ctx]
           (let [service-name (:request-body ctx)
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

(defresource users-coll
  standard-config
  :allowed-methods [:get]
  :handle-ok (fn [ctx] (data/users-coll)))

 (defresource user
   [id]
   standard-config
   :exists? (fn [ctx] (not (nil? (data/user id))))
   :handle-ok (fn [_] (data/user id)))

(defresource not-found
  standard-config
  :exists? false)

;;; Routes
(defroutes api
  (ANY "/" [] root)
  (ANY "/notifications" notif-coll)
  (ANY "/notifications/:id" [id] (notif id))
  (ANY "/services" [] services-coll)
  (ANY "/services/:id" [id] (service id))
  (ANY "/users" [] users-coll)
  (ANY "/users/:id" [id] (user id))
  (ANY "*" [] not-found))

(def handler (-> api
                 wrap-params))
