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
            [akvo.notifications.systems :as systems]
            [clojure.string :as string]
            [clojure.tools.cli :refer (parse-opts)]))

(def ^:private options
  [["-ap" "--api-port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-dh" "--ds-host HOST" "Datastore host"
    :default "localhost"]
   ["-dp" "--ds-port PORT" "Datastore port"
    :default 5002]
   ["-mq" "--ms-queue QUEUE" "Queue to consume"
    :default "akvo.service-events"]
   ["-h" "--help"]])

(defn- usage
  [options-summary]
  (string/join
   \newline
   ["Akvo notifications"
    ""
    "Options:"
    options-summary
    ""
    "Copyright (C) 2014 Stichting Akvo (Akvo Foundation)"]))

(defn- error-message [errors]
  (str "Errors while paring the command:\n\n"
       (string/join \newline errors)))

(defn- exit
  "Prints status message and exists."
  [status message]
  (println message)
  (System/exit status))

(defn -main
  "Starts the application. For now the dev-system is used for all
  possible scenarios."
  [& args]
  (let [{:keys [options errors summary]} (parse-opts args options)]
    (cond
     (:help options)  (exit 0 (usage summary))
     errors (exit 1 (error-message errors)))
    (-> options
        systems/dev-system
        component/start)))
