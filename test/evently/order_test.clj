(ns evently.order-test
  (:require [clojure.test :refer :all]
    [evently.core :refer :all])
  (:import [java.util UUID]))

(defn random-id [] (.toString (UUID/randomUUID)))
(defn now [] (System/currentTimeMillis))
(defn order [id] (aggregate id :order))

(defn status [order] (:status (state order)))
(defn new? [order] (nil? (status order)))
(defn placed? [order] (= :placed (status order)))
(defn activated? [order] (= :activated (status order)))

(defmethod handle-event :order-placed [state event]
  (assoc state :status :placed))

(defmethod handle-event :order-activated [state event]
  (assoc state :status :activated))

(defn place [order]
  (cond (new? order) (apply-change order (event (random-id) (now) :order-placed {}))
    (placed? order) order
    :else (throw (IllegalArgumentException.))))

(defn activate [order]
  (cond (placed? order) (apply-change order (event (random-id) (now) :order-activated {}))
        (activated? order) order
        :else (throw (IllegalArgumentException.))))

(deftest place-and-activate-order-test
  (let [o (-> (order "order-1")
              place
              activate)]
    (testing "Activating an order"
      (is (= :activated (status o))))))

(deftest activate-order-twice-test
  (let [o (-> (order "order-1")
              place
              activate)]
  (testing "Activating an order twice does not generate another event"
    (is (= (count (events o))
           (count (events (activate o))))))))
