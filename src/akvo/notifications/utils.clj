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
   [taoensso.timbre :refer (info)]))

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
