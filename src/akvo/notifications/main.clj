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
  ^{:doc "Akvo notificaitons use the Component framework structure."}
  akvo.notifications.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [akvo.notifications.systems :as systems]))

;; Main needs work, should get ports/host and other confs for all things that
;; needs it.
;; (Integer. (or port (System/getenv "PORT") 3000))

(defn -main [& args]
  (-> {:api-port 3000
       :ds-host  "localhost"
       :ds-port  "5002"
       :ms-queue "akvo.service-events"}
      systems/dev-system
      component/start))
