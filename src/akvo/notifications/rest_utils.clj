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

(ns akvo.notifications.rest-utils
  ^{:doc "Handy utils for crafting a Liberator API"}
  (:import [com.fasterxml.jackson.core JsonParseException])
  (:require [cheshire.core :as cheshire]
            [clojure.edn :as edn]
            [liberator.representation :refer [ring-response]]
            [liberator.core :refer [by-method]]))

;;; Setup
(def available-media-types
  ["application/json" "application/edn"])

(def error-responses
  {:404 {:reason "Resource not found."}
   :415 (format "We only speak: %s." available-media-types)
   :422 {:reason "Could not process request."}})

(defn- slurp-body
  [ctx]
  (slurp (get-in ctx [:request :body])))

;; malformed? -----------------------------------------------------------------
(defmulti malformed?
  (fn [ctx] (get-in ctx [:request :content-type]))
  :default :unsupported)

(defmethod malformed? :unsupported [_] false)
(defmethod malformed? "application/json"
  [ctx]
  (try
    [false, {:request-body (cheshire/parse-string (slurp-body ctx) true)}]
    (catch JsonParseException e [true, {:body-error "Malformed JSON"}])))
(defmethod malformed? "application/edn"
  [ctx]
  (try
    [false, {:request-body (edn/read-string (slurp-body ctx))}]
    (catch RuntimeException e [true, {:body-error "Malformed EDN"}])))

;; handle-malformed ------------------------------------------------------------
(defmulti handle-malformed
  (fn [ctx] (get-in ctx [:request :content-type])))

(defmethod handle-malformed "application/json"
  [ctx]
  (ring-response
   {:body (cheshire/generate-string {:reason (:body-error ctx "malformed")})
    :headers {"Content-Type" "application/json"}
    :status 400}))
(defmethod handle-malformed "application/edn"
  [ctx]
  (ring-response
   {:body (pr-str {:reason (:body-error ctx "malformed")})
    :headers {"Content-Type" "application/edn"}
    :status 400}))

(def standard-config
  {:allowed-methods             [:get]
   :available-media-types       available-media-types
   :malformed?                  (by-method {:get false
                                            :post (fn [ctx] (malformed? ctx))})
   :handle-malformed            (fn [ctx] (handle-malformed ctx))
   :handle-not-acceptable       (:415 error-responses)
   :handle-not-found            (:404 error-responses)
   :handle-unprocessable-entity (:422 error-responses)})

(defn processable?
  [ctx validator]
  (try
    (validator (:request-body ctx))
    (catch AssertionError e false)))
