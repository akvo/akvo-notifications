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

(ns ^{:doc "Web app that exposes a REST API"}
  akvo.notifications.web-app
  (:require
   [akvo.notifications.api-utils :refer (available-media-types
                                         handle-malformed
                                         malformed?
                                         processable?
                                         standard-config)]
   [akvo.notifications.db :as db]
   [com.stuartsierra.component :refer (Lifecycle)]
   [compojure.core :refer (defroutes ANY)]
   [clojure.pprint :refer (pprint)]
   [ring.util.request :refer (request-url)]
   [liberator.core :refer (defresource by-method)]
   [liberator.dev :refer (wrap-trace)]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]
   [taoensso.timbre :refer (info)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

(defn- get-db
  "We can grab the database via the webapp set on the request."
  [ctx]
  {:pre [(get-in ctx [:request ::webapp :db])]}
  (get-in ctx [:request ::webapp :db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General resources

(def root-map
  {:name        "akvo-notifications"
   :description "This API...."
   :supported-media-types available-media-types
   :links       [{:rel  "self"
                  :href "/"}]
   :resources   [{:name        "Notifications"
                  :description "User notifications"
                  :links       [{:rel  "self"
                                 :href "/notifications"}]}
                 {:name        "services"
                  :description "Represents Akvo internal services"
                  :links       [{:rel  "self"
                                 :href "/services"}]}
                 {:name        "users"
                  :description "Represent Akvo end users"
                  :links       [{:rel  "self"
                                 :href "/users"}]}]})

(defresource root
  standard-config

  :handle-ok root-map)

(defresource not-found
  standard-config

  :allowed-methods [:get :post]

  :post-to-missing? false

  :exists? false)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event resources

(defresource events-coll
  standard-config

  :allowed-methods [:get]

  :handle-ok
  (fn [ctx] (db/events-coll (get-db ctx)))

  :processable?
  (by-method {:get true}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services resources

(defn- create-services-validator
  [ctx]
  {:pre [(get-in ctx [:request-body :name])
         (db/valid-service-name? (get-in ctx [:request-body :name]))
         (db/service-exists? (get-db ctx)
                             (get-in ctx [:request-body :name]))]}
  true)

(defresource services-coll
  standard-config

  :allowed-methods [:get :post]

  :handle-ok
  (fn [ctx] (db/services-coll (get-db ctx)))

  :processable?
  (by-method {:get true
              :post (fn [ctx]
                      (processable? create-services-validator
                                    ctx))})

  :post!
  (fn [ctx]
    (let [service-name (:name (:request-body ctx))
          service-id (db/create-service (get-db ctx)
                                        service-name)]
      (info "service-id: " service-id)
      {::id (str service-id)}))

  :post-redirect?
  (fn [ctx]
    {:location (format "%s/%s"
                       (request-url (:request ctx))
                       (::id ctx))}))

(defresource service [id]
  standard-config

  :exists?
  (fn [ctx] (not (nil? (db/service (get-db ctx) id))))

  :handle-ok
  (fn [ctx] (db/service (get-db ctx) id)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Construct web app

(defroutes app-routes
  (ANY "/" [] root)
  (ANY "/events" [] events-coll)
  (ANY "/services" [] services-coll)
  (ANY "/services/:id" [id] (service id))
  (ANY "*" [] not-found))

(defn- wrap-app-component
  "Helper to make-hander that adds the api to the request"
  [f webapp]
  (fn [req]
    (f (assoc req ::webapp webapp))))

(defn make-handler
  "Returns a handler that adds the provided api component to the request."
  [webapp]
  (-> app-routes
      (wrap-app-component webapp)
      (wrap-trace :header :ui)))

(defrecord WebApp [port server]
  Lifecycle

  (start [this]

    (if (nil? server)
      (let [jetty (jetty/run-jetty (make-handler this)
                                   {:port port :join? false})]
        (info "; Webapp Started")
        (assoc this :server jetty))
      this))

  (stop [this]
    (when server
      (info "; WebApp Stopped")
      (.stop server))
    (assoc this :server nil)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn webapp [port]
  (map->WebApp {:port port}))
