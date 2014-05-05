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

(ns
  ^{:doc "The top level container component that will rule over it's
  dependencies."}
  akvo.notifications.app
  (:require
   [com.stuartsierra.component :refer (Lifecycle)]))

(defrecord App []
  Lifecycle

  (start [this]
    (println "; App started")
    this)

  (stop [this]
    (println "; App stopped")
    this))

(defn new-app
  "Creates a new app component."
  []
  (map->App {}))
