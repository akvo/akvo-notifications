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
  (:require
   [com.stuartsierra.component :as component]
   [akvo.notifications.systems :as systems]
   [clojure.string :as string]
   [clojure.tools.cli :refer (parse-opts)])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CLI

(def options
  [
   ;; ["-wh" "--web-host HOST" "Web service host"
   ;;  :default "localhost"]
   ["-wp" "--web-port PORT" "Web service port"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-dh" "--data-host HOST" "Datastore host"
    :default "localhost"]
   ["-dp" "--data-port PORT" "Datastore port"
    :default 5002
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-qh" "--queue-host HOST" "Queue host"
    :default "localhost"]
   ["-qp" "--queue-port PORT" "Queue port"
    :default 5672
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-qu" "--queue-user USERNAME" "Queue username"
    :default "guest"]
   ["-qc" "--queue-password PASSWORD" "Queue password"
    :default "guest"]
   ["-qv" "--queue-vhost VHOST" "Queue vhost"
    :default "/"]
   ["-qn" "--queue-name QUEUE" "Queue name"
    :default "notif.service-events"]
   ["-h" "--help"]])

(defn- usage
  [options-summary]
  (string/join
   \newline
   ["Akvo notifications"
    ""
    "usage: java -jar <path to jar> [options...]"
    "Options:"
    options-summary
    ""
    "Copyright (C) 2014 Stichting Akvo (Akvo Foundation)"]))

(defn- error-message
  [errors]
  (str "Errors while paring the command:\n\n"
       (string/join \newline errors)))

(defn- exit
  "Prints status message and exists."
  [status message]
  (println message)
  (System/exit status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application lifecycle

(def system nil)

(defn init
  [s options]
  (alter-var-root #'system (constantly (s options))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn -main
  "Starts the application. For now the dev-system is used for all
  possible scenarios."
  [& args]
  (let [{:keys [options errors summary]} (parse-opts args options)]
    (cond
     (:help options)  (exit 0 (usage summary))
     errors (exit 1 (error-message errors)))
    (init systems/dev-system options)
    (start)))
