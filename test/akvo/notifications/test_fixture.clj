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

(ns akvo.notifications.test-fixture
  (:require
   [akvo.notifications.main :as main]
   [akvo.notifications.systems :as systems]
   [clojure.tools.cli :refer (parse-opts)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Setup

(def test-options (:options (parse-opts []
                                        main/options)))

(def base-url (str "http://localhost:" (:web-port test-options)))

(def services-url (str base-url "/services"))

(defn system-fixture [f]
  (try
    (main/init systems/dev-system test-options)
    (main/start)
    (f)
    (finally (main/stop))))
