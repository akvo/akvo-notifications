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

(ns
  ^{:doc "Simple fixture data, until we hook in a real db"}
  akvo.notifications.data-store)

;; (defn tuple-vec->id-tuple
;;   "\"Vector of truples to tuples of id's\"; transforms a vector of
;;   tuples into a map with the tuples id as keywords. Example: [{:id 1
;;   :name \"Bob} {:id 2 :name \"Jane\"}] -> {:1 {:id :name \"example\"} :2
;;   {:id 2 :name \"Jane }}"
;;   [tuple-vec]
;;   (let [l (map #(assoc {} (keyword (str (:id %))) %) tuple-vec)]
;;     (into {} l)))

(defn tuple-vec->id-tuple
  [tuple-vec]
  (if (empty? tuple-vec)
    {}
    (reduce #(assoc %1 (keyword (str (:id %2))) %2) {} tuple-vec)))

;;; Services
(def services-data
  (atom [{:id    1
          :name  "akvo-rsr"
          :links [{:rel "self" :href "/services/1"}]}
         {:id    2
          :name  "akvo-flow"
          :links [{:rel "self" :href "/services/2"}]}]))

(defn create-service
  "Returns a tuple representing a service"
  [old-services name]
  (let [id (inc (count old-services))]
     {:id id
      :name name
      :links [{:rel "self"
               :href (format "/services/%s" id)}]}))

(defn add-new-service
  "Add service and return new collection"
  [services name]
  (swap! services conj (create-service @services name)))

(defn add-service
  "Update the collection with new item, count and return id. This is not
  safe code; needs to be updated when we hook in a proper data
  store. Ids should not be counted and validation of new services needs
  to be performed. We should not allow two services with the same name."
  [name]
  (let [old-services services-data
        new-services (add-new-service old-services name)]
    (count new-services)))

(defn services-coll
  "Return all services"
  []
  @services-data)


(defn service
  "Return details about a service"
  [id]
  ((keyword id) (tuple-vec->id-tuple @services-data)))


;;; Users
(def users-data
  (atom [{:id    1
          :name  "Bob"
          :links [{:rel "self"
                   :href "/users/1"}]}
         {:id    2
          :name  "Jane"
          :links [{:rel "self"
                   :href "/users/1"}]}]))

(defn users-coll []
  @users-data)

(defn user
  "Return details about a service"
  [id]
  ((keyword id) (tuple-vec->id-tuple @users-data)))
