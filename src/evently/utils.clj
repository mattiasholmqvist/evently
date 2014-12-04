(ns evently.utils
  (:import [java.util UUID]))

(defn random-id [] (.toString (UUID/randomUUID)))

(defn now [] (System/currentTimeMillis))
