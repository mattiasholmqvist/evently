(ns evently.spec-test
  (:require [clojure
             [spec :as spec]
             [test :as t :refer [deftest is]]]
            [clojure.spec.test :as s]
            [evently.core :as e]))

(def ^:dynamic *num-tests* 15)

(deftest all-spec
  (let [r (s/test)]
    (println r)
    (is (true? (:result r)))))

(comment
  (defn spec-test
    "A helper for running tests with clojure.spec.test"
    [sym]
    (-> sym
      (s/test {:num-tests *num-tests*})
      :fail
      zero?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (deftest safe-inc
    (is (spec-test #'evently.core/safe-inc)))

  (deftest aggregate-spec
    (is (spec-test #'evently.core/aggregate)))

  (deftest events-spec
    (is (spec-test #'evently.core/events)))

  (deftest version-spec
    (is (spec-test #'evently.core/version)))

  (deftest timestamp-spec
    (is (spec-test #'evently.core/timestamp)))

  (deftest state-spec
    (is (spec-test #'evently.core/state)))

  (deftest event-dispatcher-spec
    (is (spec-test #'evently.core/event-dispatcher)))

  (deftest next-version-spec
    (is (spec-test #'evently.core/next-version)))

  (deftest make-event-spec
    (is (spec-test #'evently.core/make-event)))

;;; TODO

  #_(deftest apply-metadata-from-spec
      (is (spec-test #'evently.core/apply-metadata-from)))

  #_(deftest apply-state-from-spec
      (is (spec-test #'evently.core/apply-state-from)))

  #_(deftest apply-change-spec
      (is (spec-test #'evently.core/apply-change)))

  #_(deftest replay-event-spec
      (is (spec-test #'evently.core/replay-event)))

  #_(deftest materialze-spec
      (is (spec-test #'evently.core/materialize)))

  #_(deftest emit-event-spec
      (is (spec-test #'evently.core/emit-event)))

  )
