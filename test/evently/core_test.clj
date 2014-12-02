(ns evently.core-test
  (:require [clojure.test :refer :all]
            [evently.core :refer :all]))

(deftest create-aggregate-test
  (let [aggregate-id "some-id"
        ar (aggregate aggregate-id :some-thing)]
  (testing "Creating an aggregate yields 0 events"
    (is (empty? (events ar))))))

(defmethod handle-event :something-happened-to-thing
  [state something-happened-event]
  (merge state (:data something-happened-event)))

(deftest apply-two-events-test
  (let [last-timestamp 10
        ar (-> "some-id"
           (aggregate :thing)
           (apply-change (event "event-1" 1 :thing-created {}))
           (apply-change (event "event-2" last-timestamp :something-happened-to-thing {:amount 20})))]

  (testing "Adding two events increments aggregate version with two"
    (is (= 3 (version ar))))

  (testing "Retrieves the events in insertion-order"
    (is (= "event-1" (:event-id (first  (events ar)))))
    (is (= "event-2" (:event-id (second (events ar)))))

  (testing "Aggregate root timestamp is equal to timestamp of the last event"
    (is (= last-timestamp (timestamp ar))))

  (testing "Aggregate root state updates is configurable by injected handlers"
    (is (= {:amount 20} (state ar)))))))
