(ns akvo.notifications.main-test
  (:require [akvo.notifications.main :as main]
            [clojure.test :refer :all]
            [clojure.tools.cli :refer (parse-opts)]))

(defn get-mode
  [options]
  (get-in (parse-opts [options] main/options) [:options :mode]))

(deftest options

  (testing "Default mode"
    (let [production-mode (get-mode "")
          dev-mode (get-mode "--mode=dev")]
      (is (= production-mode "production"))
      (is (= dev-mode "dev")))))
