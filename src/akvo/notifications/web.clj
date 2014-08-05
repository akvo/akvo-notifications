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

(ns ^{:doc "Component that exposes a REST API."}
  akvo.notifications.web
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require [bidi.bidi :refer (compile-route
                               make-handler) :as bidi]
            [cheshire.core :as cheshire]
            [clojure.edn :as edn]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :refer (Lifecycle)]
            [liberator.core :refer (defresource by-method)]
            [liberator.dev :refer (wrap-trace)]
            [liberator.representation :refer (ring-response)]
            [org.httpkit.server :refer (run-server)]
            [plumbing.core :refer (?>)]
            [ring.util.request :refer (request-url)]
            [taoensso.timbre :refer (debug)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utility

(defn db-backend
  "Grabs backend from provided context."
  [ctx]
  (get-in ctx [:request ::webapp :store :backend]))

(defn db-data
  "Grabs data from provided context."
  [ctx]
  (get-in ctx [:request ::webapp :store :data]))

(defn get-route-id
  "Get route id from provided context."
  [ctx]
  (read-string (get-in ctx [:request :route-params :id])))

(defmacro db<
  "Db chew - macro that feeds configured data store backend with
  function calls. Expects the functions as a symbol without
  namespace (needs to be beutified)."
  [fn & rest]
  (if (nil? rest)
    `(do
       ((resolve (symbol (db-backend ~@(keys &env))
                         (str ~fn)))
        (db-data ~@(keys &env))))
    `(do
       ((resolve (symbol (db-backend ~@(keys &env))
                         (str ~fn)))
        (db-data ~@(keys &env))
        ~@rest))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Liberator utility

(def available-media-types
  "JSON & EDN."
  ["application/json" "application/edn"])

(def ^:private error-responses
  {:404 {:reason "Resource not found."}
   :415 (format "We only speak: %s." available-media-types)
   :422 {:reason "Could not process request."}})

(defn- slurp-body
  [ctx]
  (slurp (get-in ctx [:request :body])))

;;; malformed?

(defmulti malformed?
  "General function that verify that JSON and EDN requests are well formed."
  (fn [ctx] (get-in ctx [:request :content-type]))
  :default :unsupported)

(defmethod malformed? :unsupported [_] false)
(defmethod malformed? "application/json"
  [ctx]
  (try
    [false {:request-body (cheshire/parse-string (slurp-body ctx) true)}]
    (catch JsonParseException e [true {:body-error "Malformed JSON"}])))
(defmethod malformed? "application/edn"
  [ctx]
  (try
    [false {:request-body (edn/read-string (slurp-body ctx))}]
    (catch RuntimeException e [true {:body-error "Malformed EDN"}])))

;;; handle-malformed

(defmulti handle-malformed
  "General function that provides error messages for JSON and EDN requests."
  (fn [ctx] (get-in ctx [:request :content-type])))

(defmethod handle-malformed "application/json"
  [ctx]
  (ring-response
   {:body    (cheshire/generate-string {:reason (:body-error ctx "malformed")})
    :headers {"Content-Type" "application/json"}
    :status  400}))
(defmethod handle-malformed "application/edn"
  [ctx]
  (ring-response
   {:body    (pr-str {:reason (:body-error ctx "malformed")})
    :headers {"Content-Type" "application/edn"}
    :status  400}))

;;; processable?

(defn processable?
  "Function that takes a validator function and a context. The validator
  function is expexted to either return true or throw an
  AssertionError. Using :pre makes this simple."
  [validator ctx]
  (try
    (validator ctx)
    (catch AssertionError e false)))

;;; defaults

(def standard-config
  "Provides a set of default configs, to be used with liberator routes."
  {:allowed-methods             [:get]
   :available-media-types       available-media-types
   :malformed?                  (by-method {:get false
                                            :post (fn [ctx] (malformed? ctx))})
   :handle-malformed            (fn [ctx] (handle-malformed ctx))
   :handle-not-acceptable       (:415 error-responses)
   :handle-not-found            (:404 error-responses)
   :handle-unprocessable-entity (:422 error-responses)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Root & not found resources

(def root-map
  "Used for serving the home page."
  {:name "Akvo notifications"})

(defresource root
  standard-config

  :handle-ok root-map)

(defresource not-found
  standard-config

  :allowed-methods [:get :post]

  :post-to-missing? false

  :exists? false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Events resources

(defresource events-coll
  standard-config

  :handle-ok
  (fn [ctx]
    (db< 'events-coll)))

(defresource events-nth
  standard-config

  :exists?
  (fn [ctx]
    (not (nil? (db< 'events-nth
                    (get-route-id ctx)))))

  :handle-ok
  (fn [ctx]
    (db< 'events-nth (get-route-id ctx))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Services resources

(defn- create-service-validator
  [ctx]
  {:pre [(get-in ctx [:request-body :name])
         (not (db< 'service-exists? (get-in ctx [:request-body :name])))]}
  true)

(defresource services-coll
  standard-config

  :allowed-methods [:get :post]

  :handle-ok
  (fn [ctx]
    (db< 'services-coll))

  :processable?
  (by-method {:get true
              :post (fn [ctx] (processable? create-service-validator ctx))})

  :post!
  (fn [ctx]
    {::id (str (db< 'create-service (get-in ctx [:request-body :name])))})

  :post-redirect?
  (fn [ctx] {:location (format "%s/%s" (request-url (:request ctx))
                              (::id ctx))}))

(defresource services-nth
  standard-config

  :exists?
  (fn [ctx]
    (not (nil? (db< 'services-nth
                    (get-route-id ctx)))))

  :handle-ok
  (fn [ctx]
    (db< 'services-nth (get-route-id ctx))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Users resources

(defresource users-coll
  standard-config

  :handle-ok
  (fn [ctx]
    (db< 'users-coll)))

(defresource users-nth
  standard-config

  :exists?
  (fn [ctx]
    (not (nil? (db< 'users-nth
                    (get-route-id ctx)))))

  :handle-ok
  (fn [ctx]
    (db< 'users-nth (get-route-id ctx))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Handler

(def app-routes ; No compile-route https://github.com/juxt/bidi/issues/17
  "Defines http resources."
  ["/" {""         root
        "events"   {""        events-coll
                    ["/" :id] events-nth}
        "services" {""        services-coll
                    ["/" :id] services-nth}
        "users"    {""        users-coll
                    ["/" :id] users-nth}
        [#".*"]    not-found}])

(defn- wrap-app-component
  "Helper to make-hander that adds the api to the request."
  [f component]
  (fn [req]
    (f (assoc req ::webapp component))))

(defn handler
  "Returns a handler with the web app component assoc:ed to the request."
  [component]
  (-> (make-handler app-routes)
      (wrap-app-component component)
      (?> (= (:mode component) "dev")
          (wrap-trace :header :ui))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Web component

(defrecord Web [port server mode]
  Lifecycle

  (start [this]
    (if (nil? server)
      (let [kit (run-server (handler this)
                            {:port port :join? false})]
        (debug "Web component started")
        (assoc this :server kit))
      this))

  (stop [this]
    (when server
      (server)
      (debug "Web component stopped"))
    (assoc this :server nil)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn web
  "Initialize the web component."
  [mode port]
  (map->Web {:mode mode
             :port port}))
