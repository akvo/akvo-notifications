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

(ns ^{:doc "Main namespace that deals with the service CLI & life cycle."}
  akvo.notifications.main
  (:gen-class)
  (:require [akvo.notifications.systems :as systems]
            [clojure.string :as string]
            [clojure.pprint :refer (pprint)]
            [clojure.tools.cli :refer (parse-opts)]
            [taoensso.timbre :refer (info set-level!)]
            [com.stuartsierra.component :as component]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CLI

(def options
  "CLI options."
  [["-m" "--mode MODE" "Execution mode. production or dev"
    :default "production"]
   ["-wp" "--web-port PORT" "Web service port"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-b" "--backend store" "Storage backend"
    :default "store-mem"]
      ["-qh" "--queue-host HOST" "Queue host"
    :default "localhost"]
   ["-qp" "--queue-port PORT" "Queue port"
    :default 5672
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000)]]
   ["-qu" "--queue-user USERNAME" "Queue username"
    :default "guest"]
   ["-qpa" "--queue-password PASSWORD" "Queue password"
    :default "guest"]
   ["-qv" "--queue-vhost VHOST" "Queue vhost"
    :default "/"]
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

(defn set-logging-level
  "Based on mode (production / dev) set logging level."
  [options]
  ;; Timbre ordered levels: [:trace :debug :info :warn :error :fatal :report]
   (if (= (:mode options) "production")
     (set-level! :info)
     (set-level! :trace)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Application lifecycle

(def system
  "The one global system var."
  nil)

(defn init
  "Set logging level & init the system."
  [s options]
  (set-logging-level options)
  (alter-var-root #'system (constantly (s options))))

(defn start
  "Start the system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Stop the system."
  []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn -main
  "Main entry point."
  [& args]
  (let [{:keys [options errors summary]} (parse-opts args options)]
    (cond
     (:help options)  (exit 0 (usage summary))
     errors (exit 1 (error-message errors)))
    (init systems/production-system options)
    (start)))
