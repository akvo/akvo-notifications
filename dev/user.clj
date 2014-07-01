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
  ^{:doc "Development workflow from Stuart Sierras author of the Component
  framework (https://github.com/stuartsierra/component)"}
  user
  (:require [akvo.notifications.main :as main]
            [akvo.notifications.systems :refer (production-system)]
            [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.javadoc :refer (javadoc)]
            [clojure.pprint :refer (pprint)]
            [clojure.reflect :refer (reflect)]
            [clojure.repl :refer (apropos dir doc find-doc pst source)]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            [clojure.tools.cli :refer (parse-opts)]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.core :as rmq]
            [langohr.queue :as lq]))


(defn publish-draft
  ""
  [{:keys [content-type timestamp type] :as meta} draft]
  (let [conn     (rmq/connect)
        chan     (lch/open conn)
        exchange ""
        queue    "notif.drafts"
        message  (cheshire/generate-string draft)]
    (lq/declare chan queue :exclusive false :auto-delete false)
    (lb/publish chan exchange queue message
                :content-type content-type
                :type type
                :timestamp (c/to-date timestamp))))

;; (publish-draft {:content-type "application/json"
;;                :timestamp    (c/to-long (t/now))
;;                :type         "start-subscription"}
;;               {:item    "project-7"
;;                :suid    4
;;                :email   "tom@example.com"
;;                :name    "Tom Hope"
;;                :service "akvo-dash"})

;; (publish-draft {:content-type "application/json"
;;                :timestamp    (c/to-long (t/now))
;;                :type         "end-subscription"}
;;               {:item    "project-7"
;;                :suid    4
;;                :email   "tom@example.com"
;;                :name    "Tom Hope"
;;                :service "akvo-dash"})

;; (publish-draft {:content-type "application/json"
;;                 :timestamp    (c/to-long (t/now))
;;                 :type         "project-donation"}
;;                {:item    "project-7"
;;                 :currency "eu"
;;                 :amount 52
;;                 :service "akvo-dash"})

;; (publish-draft {:content-type "application/json"
;;                 :timestamp    (c/to-long (t/now))
;;                 :type         "user-notified"}
;;                {:email  "tom@example.com"
;;                 :events [4 5]})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Life cycle

(defn init []
  (->> (parse-opts ["--mode=dev"] main/options)
       (:options)
       (main/init production-system)))

(defn start []
  (main/start))

(defn stop []
  (main/stop))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
