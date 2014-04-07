(ns
  ^{:doc "The container component that will rule over it's dependencies."}
  akvo.notifications.app
  (:require
   [com.stuartsierra.component :refer (Lifecycle system-map using)]))

(defrecord App []
  Lifecycle

  (start [this]
    (println "; App started!")
    this)

  (stop [this]
    (println "; App stopped!")
    this))

(defn new-app []
  (map->App {}))
