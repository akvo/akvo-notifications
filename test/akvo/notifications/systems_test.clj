(ns akvo.notifications.systems-test
  (:require [akvo.notifications.systems :as systems]
            [akvo.notifications.main :as main]
            [clj-http.client :as httpc]
            [clojure.tools.cli :refer (parse-opts)]
            [clojure.test :refer :all]))


;; (def test-options (:options (parse-opts []
;;                                         main/options)))

;; (def base-url (str "http://localhost:" (:web-port test-options)))

;; (deftest lifecycle
;;   (testing "Starting, stopping & starting system"
;;     (let [resp (do
;;                  (main/init systems/dev-system test-options)
;;                  (main/start)
;;                  (main/stop)
;;                  (main/start)
;;                  (httpc/get base-url {:accept "application/edn"}))]

;;       (is (= (:status resp) 200))
;;       (main/stop))))
