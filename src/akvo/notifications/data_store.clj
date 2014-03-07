;  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

(ns
  ^{:doc "Simple fixture data, should be seen as a data store until we hook in
  a real db"}
  akvo.notifications.data-store)

(def data
  ^{:doc "Dummy data"}
  {:1 {:name "akvo-rsr"
       :token "abc123"}
   :2 {:name "akvo-flow"
       :token "def456"}})

(defn services-details [service]
  "Returns the service details with the service id added."
  (assoc (data (keyword service)) :id (Integer. service)))

                                        ; (services-details "1")

(defn services []
  "Return a list of all services."
  data)

(defn subscribe
  [service entity & users]
  (print (format "Subscribe %s to %s:%s" users service entity)))
