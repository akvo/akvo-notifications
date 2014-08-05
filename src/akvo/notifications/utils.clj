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

(ns ^{:doc "Utility functions."}
  akvo.notifications.utils
  (:require [clojure.string :refer (blank?)]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; RabbitMQ

(defn build-vhost
  "Handle the different cases of empty / or defined vhost."
  [vhost]
  (cond
   (blank? vhost) "/%2F"
   (= "/" vhost)  "/%2F"
   :else          vhost))

(defn setup-handler
  "Setup langohr handler."
  [channel connection queue handler]
  {:pre [(not (nil? channel))
         (not (nil? connection))]}
  (lq/declare channel queue :exclusive false :auto-delete false)
  (lc/subscribe channel queue handler :auto-ack true))
