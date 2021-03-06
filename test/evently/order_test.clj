(ns evently.order-test
  (:require [clojure.test :refer :all]
            [evently.utils :refer :all]
            [evently.core :refer :all]))

(defn order      [id]      (aggregate id :order))
(defn status     [order]   (:status (state order)))
(defn status?    [p order] (p order))
(defn new?       [order]   (nil? (status order)))
(defn placed?    [order]   (= :placed (status order)))
(defn activated? [order]   (= :activated (status order)))

(defn- cannot-place [order]
  (IllegalArgumentException. (str "Can only place new orders. Order is " (status order))))

(defn- cannot-activate [order]
  (IllegalArgumentException. (str "Can only activate placed orders. Order is " (status order))))

(defn- valid-order-placement-info [order-info]
  order-info)

(defn place [order customer-information order-lines total-price]
  (condp status? order
    new?    (emit-event order :order-placed
                        (valid-order-placement-info
                         {:customer-info customer-information
                          :order-lines   order-lines
                          :total-price   total-price}))
    placed? order
    (throw (cannot-place order))))

(defn activate [order]
  (condp status? order
    placed?    (emit-event order :order-activated)
    activated? order
    (throw (cannot-activate order))))

(defn- update-order-status [order-state status]
  (assoc order-state :status status))

(defmethod handle-event :order-placed [order-state order-placed-event]
  (update-order-status order-state :placed))

(defmethod handle-event :order-activated [order-state order-activated-event]
  (update-order-status order-state :activated))

;; TESTS

(def example-customer {:name  "John Doe"
                       :email "john.doe@example.com"})

(def example-order-lines [{:product-id (random-id)
                           :title      "Some book"
                           :quantity   10
                           :unit-price 13.23}])

(def example-total-price 132.30)

(deftest place-and-activate-order-test
  (let [new-order       (order (random-id))
        activated-order (-> new-order
                          (place example-customer example-order-lines example-total-price)
                          activate)]
    (testing "Activating an order"
      (is (= :activated (status activated-order))))))

(deftest activate-order-twice-test
  (let [new-order              (order (random-id))
        placed-order           (place new-order example-customer example-order-lines example-total-price)
        double-activated-order (-> placed-order activate activate)]
  (testing "Activating an order twice does not generate another event"
    (is (= (+ 1 (count (events placed-order)))
           (count (events double-activated-order)))))))
