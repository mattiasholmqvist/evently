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
        aggregate-id "some-id"
        ar (-> aggregate-id
           (aggregate :thing)
           (apply-change (make-event "event-1" 1 :thing-created {}))
           (apply-change (make-event "event-2" last-timestamp :something-happened-to-thing {:amount 20})))]

  (testing "Adding two events increments aggregate version with two"
    (is (= 2 (version ar))))

  (testing "Retrieves the events in insertion-order"
    (is (= "event-1" (:event-id (first  (events ar)))))
    (is (= "event-2" (:event-id (second (events ar)))))

  (testing "Aggregate root timestamp is equal to timestamp of the last event"
    (is (= last-timestamp (timestamp ar))))

  (testing "Aggregate root state updates is configurable by injected handlers"
    (is (= {:amount 20} (state ar)))))))

(deftest materialize-test
  (let [aggregate-id "ar-1"
        uncommitted (->
                      (aggregate aggregate-id :thing)
                      (apply-change (make-event "event-1" 1 :thing-created {}))
                      (apply-change (make-event "event-2" 2 :something-happened-to-thing {:state-key :first-value}))
                      events)
        aggregate (materialize uncommitted :thing)

        overwritten-aggregate (-> aggregate
                                  (apply-change (make-event "event-3" 2 :something-happened-to-thing {:state-key :overwritten-value}))
                                  events
                                  (materialize :thing))]
  (testing "Materializing aggregate from events"
    (is (= 0 (count (events aggregate))))
    (is (= 2 (version aggregate)))
    (is (= {:state-key :first-value} (state aggregate)))
    (is (= {:state-key :overwritten-value} (state overwritten-aggregate))))))
