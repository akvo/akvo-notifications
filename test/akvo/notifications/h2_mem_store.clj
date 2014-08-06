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

(ns akvo.notiticactions.h2-mem-store
  (:require [clojure.test :refer :all])
  (:import [org.h2.mvstore MVStore MVStore$Builder]))


(deftest mem-store
  (testing "openMap"
    (let [store (->
                  (MVStore$Builder.)
                  (.fileName nil) ;; implies in-memory
                  (.open))]
      
      (let [users (.openMap store "users")]
        (is (empty? users) "Users is empty"))
      
      (let [users (.openMap store "users")
            _ (.put users :email "nobody@akvo.org")
            tx (.commit store)]
        (is (= 1 tx) "First transaction")
        (is (= 1 (count users)) "Number of users")
        (is (= "nobody@akvo.org" (.get users :email)) "We can use keywords")))))
