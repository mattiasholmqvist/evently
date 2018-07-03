(ns evently.spec-test
  (:require [clojure
             [spec :as spec]
             [test :as t :refer [deftest is]]]
            [clojure.spec.test :as stest]
            [evently.core :as e]))

(def ^:dynamic *num-tests* 15)

(deftest specs
  (-> 'evently.core
    stest/enumerate-namespace
    (stest/check {:num-tests *num-tests*})
    stest/summarize-results))
