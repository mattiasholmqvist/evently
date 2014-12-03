(ns evently.order-test
  (:require [clojure.test :refer :all]
    [evently.core :refer :all]))

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
  (cond (new? order) (emit-event order :order-placed {})
    (placed? order) order
    :else (throw (IllegalArgumentException.))))

(defn activate [order]
  (cond (placed? order) (emit-event order :order-activated {})
        (activated? order) order
        :else (throw (IllegalArgumentException. (str "Can only activate placed orders. Order is " (status order))))))

(deftest place-and-activate-order-test
  (let [new-order (order (random-id))
        activated-order (-> new-order place activate)]
    (testing "Activating an order"
      (is (= :activated (status activated-order))))))

(deftest activate-order-twice-test
  (let [new-order (order (random-id))
        placed-order (place new-order)
        double-activated-order (-> placed-order activate activate)]
  (testing "Activating an order twice does not generate another event"
    (is (= (+ 1 (count (events placed-order)))
           (count (events double-activated-order)))))))
