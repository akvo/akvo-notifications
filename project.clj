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

(defproject akvo-notifications "0.1.0-SNAPSHOT"
  :description "Akvo-notifications"
  :url "https://github.com/akvo/akvo-notifications"
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl"}
  :dependencies [[cheshire "5.3.1"]
                 [com.novemberain/langohr "2.8.2"]
                 [com.stuartsierra/component "0.2.1"]
                 [compojure "1.1.6"]
                 [liberator "0.11.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-jetty-adapter "1.2.2"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [ring-mock "0.1.5"]]
                   :source-paths ["dev"]}}
  :main akvo.notifications.main)
