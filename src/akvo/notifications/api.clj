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

(ns ^{:doc "REST:ish HTTP API for Internal services to consume."}
  akvo.notifications.api
  (:require
   [akvo.notifications.api-utils :refer (available-media-types
                                         handle-malformed
                                         malformed?
                                         processable?
                                         standard-config)]
   [akvo.notifications.datastore-mem :as ds]
   [com.stuartsierra.component :refer (Lifecycle)]
   [compojure.core :refer (defroutes ANY)]
   [clojure.pprint :refer (pprint)]
   [ring.util.request :refer (request-url)]
   [liberator.core :refer (defresource by-method)]
   [liberator.dev :refer (wrap-trace)]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map

(def api-map
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers

(defn get-ds [ctx]
  {:pre [(get-in ctx [:request ::api :ds])]}
  (get-in ctx [:request ::api :ds]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General resources

(defresource root
  standard-config
  :handle-ok api-map)

(defresource not-found
  standard-config
  :allowed-methods [:get :post]
  :post-to-missing? false
  :exists? false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Notifications

(defresource notif-coll
  standard-config
  :exists? false)

(defresource notif
  [id]
  standard-config
  :exists? false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services

(defn- existing-services
  "Service validator helper function"
  [coll]
  (reduce #(assoc %1 (:name %2) (:id %2)) {} coll))

(defn- services-validator
  "Validator fuction for adding new services. First "
  [ctx]
  {:pre [(get-in ctx [:request-body :name])
         (not (contains? (existing-services
                          @(:services (get-ds ctx)))
                         (get-in ctx [:request-body :name])))]}
  true)

(defresource services-coll
  standard-config
  :allowed-methods [:get :post]
  :handle-ok (fn [ctx] (ds/services-coll (get-ds ctx)))
  :processable? (by-method {:get true
                            :post (fn [ctx]
                                    (processable? services-validator ctx))})
  :post! (fn [ctx]
           (let [service-name (:name (:request-body ctx))
                 service-id (ds/add-service (get-ds ctx)
                                            service-name)]
             {::id (str service-id)}))
  :post-redirect? (fn [ctx]
                    {:location (format "%s/%s"
                                       (request-url (:request ctx))
                                       (::id ctx))}))

(defresource service
  [id]
  standard-config
  :exists? (fn [ctx] (not (nil? (ds/service (get-ds ctx) id))))
  :handle-ok (fn [ctx] (ds/service (get-ds ctx) id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users

(defresource users-coll
  standard-config
  :allowed-methods [:get]
  :handle-ok (fn [ctx] (ds/users-coll (get-ds ctx)))
  :processable? (by-method {:get true}))

 (defresource user
   [id]
   standard-config
   :exists? (fn [ctx] (not (nil? (ds/user (get-ds ctx) id))))
   :handle-ok (fn [ctx] (ds/user (get-ds ctx) id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Construct web app

(defroutes app-routes
  (ANY "/" [] root)
  (ANY "/notifications" [] notif-coll)
  (ANY "/notificaitons/:id" [id] (notif id))
  (ANY "/services" [] services-coll)
  (ANY "/services/:id" [id] (service id))
  (ANY "/users" [] users-coll)
  (ANY "/users/:id" [id] (user id))
  (ANY "*" [] not-found))

(defn wrap-app-component [f api]
  (fn [req]
    (f (assoc req ::api api))))

(defn make-handler [api]
  (-> app-routes
      (wrap-app-component api)
      (wrap-trace :header :ui))) ; Turn of for production !!!!!!!!!!!!!!!!!!!!

(defrecord API [port server]
  Lifecycle

  (start [this]

    (if (= server nil)
      (let [jetty (jetty/run-jetty (make-handler this)
                                   {:port port :join? false})]
        (println "; API Started.")
        (assoc this :server jetty))
      this))

  (stop [this]
    (println "Stopping API...")
    (when server
      (.stop server))
    (assoc this :server nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn new-api
  [port]
  (map->API {:port port}))
