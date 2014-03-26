(ns akvo.notifications.rest-api-test
  (:require [akvo.notifications.rest-api :as api]
            [akvo.notifications.data-store :as data]
            [ring.mock.request :refer :all]
            [cheshire.core :as cheshire]
            [clojure.edn :as edn]
            [midje.sweet :refer :all]))

(fact "/"
      (:status (api/handler (request :get "/")))
      => 200)

(fact "/notifications"
      (:status (api/handler (request :get "/notifications")))
      => 404)

(fact "/notifications/1"
      (:status (api/handler (request :get "/notifications/1")))
      => 404)

(fact "/services and non supported media type"
      (:status (api/handler (request :get "/services")))
      => 200
      (:status (api/handler (-> (request :get "/services")
                                (header "accept" "application/xml"))))
      => 406)

(fact "/services/1"
      (:status (api/handler (request :get "/services/1")))
      => 200)

(fact "Adding a new service, with different media types"
      (count (cheshire/parse-string
              (:body (api/handler (-> (request :get "/services")
                                      (content-type "application/json"))))))
      => 2
      (:status (api/handler (-> (request :post "/services")
                                (body "{")
                                (content-type "application/json"))))
      => 400
      (:status (api/handler (-> (request :post "/services")
                                (body (cheshire/generate-string
                                       {:nam "akvo-rsr"}))
                                (content-type "application/json"))))
      => 422
      (:status (api/handler (-> (request :post "/services")
                                (body (cheshire/generate-string
                                       {:name "akvo-dash"}))
                                (content-type "application/json"))))
      => 303
      (count (edn/read-string
              (:body (api/handler (-> (request :get "/services")
                                      (header "accept" "application/edn")
                                      (content-type "application/edn"))))))
      => 3)

(fact "/users"
      (:status (api/handler (request :get "/users")))
      => 200)

(fact "/users/1"
      (:status (api/handler (request :get "/users/1")))
      => 200)

(fact "Not found"
      (:status (api/handler (request :get "/not-found")))
      => 404
      (:status (api/handler (request :post "/not-found")))
      => 404
      (:status (api/handler (request :put "/not-found")))
      => 405)
