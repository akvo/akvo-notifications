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
                 [com.fasterxml.jackson.core/jackson-core "2.3.1-SNAPSHOT"]
                 [com.novemberain/langohr "2.7.1"]
                 [compojure "1.1.3"]
                 [liberator "0.11.0"]
                 [org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {
         :handler akvo.notifications.rest-api/handler
         :init akvo.notifications.core/init
         :destroy akvo.notifications.core/destroy
         :port 3000
         :auto-reload? true}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]
                                  [midje "1.6.3"]]}}
  :main akvo.notifications.core
  :aot [akvo.notifications.core])
