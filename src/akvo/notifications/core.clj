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

(ns akvo.notifications.core
  ^{:doc "Notification service consist of a REST API which other services can
  consume but also an input channel via service events sent over a
  message queue.

  The service can be started either A as a jar or B via lein (for
  development)

  A: This is the production use case and we hook in core/init from main
  to start message queue event handlers.

  B: If started with lein ring server we use the ring app handle, but we
  also need to initialize the message queue event handlers. This is done
  via the init hook defined in project.clj."}
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [akvo.notifications.rest-api :as api]
            [akvo.notifications.event-handlers :as eh]))

(defn init
  "Init the event handlers"
  []
  (eh/init))

(defn destroy
  "Clen up after us."
  []
  (eh/destroy))

(defn -main
  ""
  [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 3000))]
    (init)
    (run-jetty (var api/handler) {:join false :port port})))
