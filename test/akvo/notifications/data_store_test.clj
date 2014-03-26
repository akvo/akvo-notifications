(ns akvo.notifications.data-store-test
  (:require [akvo.notifications.data-store :refer :all]
            [midje.sweet :refer :all]))

(fact "`tuple-vec->id-tuple converts..."
      (tuple-vec->id-tuple [])
      => {}
      (tuple-vec->id-tuple {})
      => {}
      (tuple-vec->id-tuple [{:id 1 :name "Bob"}])
      => {:1 {:id 1 :name "Bob"}}
      (tuple-vec->id-tuple [{:id 1 :name "Bob"} {:id 2 :name "Jane"}])
      => {:1 {:id 1 :name "Bob"}
          :2 {:id 2 :name "Jane"}})
