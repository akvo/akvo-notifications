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

(ns ^{:doc "Utility functions"}
  akvo.notifications.utils
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require
   [cheshire.core :as cheshire]
   [clojure.edn :as edn]
   [clojure.string :refer (blank?)]
   [langohr.consumers :as lc]
   [langohr.queue :as lq]
   [liberator.core :refer (by-method)]
   [liberator.representation :refer (ring-response)]
   [taoensso.timbre :refer (info)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Liberator

(def available-media-types
  "JSON & EDN"
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
;;; RabbitMQ

(defn setup-handler
  [channel connection queue handler]
  {:pre [(not (nil? channel))
         (not (nil? connection))]}
  (lq/declare channel queue :exclusive false :auto-delete false)
  (lc/subscribe channel queue handler :auto-ack true))

(defn vhost
  [queue-vhost]
  (cond
   (blank? queue-vhost) "/%2F"
   (= "/" queue-vhost)  "/%2F"
   :else                queue-vhost))


(defmulti read-message
  "..."
  (fn [meta message] (:content-type meta))
  :default :unsupported)

(defmethod read-message :unsupported [meta message]
  (info "Got unprocessable message" meta message))

(defmethod read-message "application/json"
  [meta message]
  (try
    (cheshire/parse-string message true)
    (catch JsonParseException e (info "Could not parse message of type: "
                                      (:content-type meta)
                                      "\nMessage: " message))))

(defmethod read-message "application/edn"
  [meta message]
  (try
    (edn/read-string message)
    (catch RuntimeException e (info "Coudl not parse message of type: "
                                    (:content-type meta)
                                    "\nMessage: " message))))
