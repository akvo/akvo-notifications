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

(defproject akvo-notifications "0.4.1-SNAPSHOT"
  :description "Akvo service that turns business events into user notifications"
  :url "https://github.com/akvo/akvo-notifications"
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl"}
  :dependencies [[bidi "1.10.4"]
                 [cheshire "5.3.1"]
                 [clj-time "0.7.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [com.taoensso/timbre "3.1.6"]
                 [compojure "1.1.8"]
                 [http-kit "2.1.18"]
                 [liberator "0.11.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [prismatic/plumbing "0.3.2"]
                 [prismatic/schema "0.2.4"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [com.h2database/h2 "1.4.180"]]
  :profiles {:dev {:dependencies [[clj-http "0.9.1"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}}
  :codox {:exclude [user]}
  :main akvo.notifications.main)
