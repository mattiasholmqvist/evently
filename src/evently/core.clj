(ns evently.core
  (:require [clojure.spec :as spec]
            [evently.utils :refer [random-id now]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helper functions

(spec/fdef safe-inc
  :args
  (spec/cat :x number?)
  :ret
  number?)

(defn safe-inc [x]
  ((fnil inc 0) x))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; General-purpose Specs

(spec/def ::event-id string?)
(spec/def ::timestamp number?) ;; TODO: should this be `inst?`
(spec/def ::data associative?)

(spec/def ::event
  (spec/keys
   :req-un
   [::event-id
    ::timestamp
    ::type
    ::data]))


(spec/def ::id (spec/or :uuid uuid? :string string?))
(spec/def ::type keyword?)
(spec/def ::uncommitted-events (spec/* ::event))
(spec/def ::version integer?)
(spec/def ::state associative?)

(spec/def ::aggregate-root
  (spec/keys
   :req-un
   [::id
    ::type
    ::uncommitted-events
    ::version
    ::state]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Core Functions

(spec/fdef aggregate
  :args
  (spec/cat :aggregate-id ::id
            :type ::type)
  :ret
  ::aggregate-root)

(defn aggregate
  "Creates an event-sourced aggregate root"
  [aggregate-id type]
  {:id                 aggregate-id
   :type               type
   :uncommitted-events []
   :version            0
   :state              {}})


(spec/fdef events
  :args
  (spec/cat :aggregate-root ::aggregate-root)
  :ret
  ::uncommitted-events)

(defn events
  "Returns the uncommitted events of the aggregate root in insertion-order"
  [aggregate-root]
  (:uncommitted-events aggregate-root))


(spec/fdef version
  :args
  (spec/cat :aggregate-root ::aggregate-root)
  :ret
  ::version)

(defn version
  "Returns the version of the aggregate root"
  [aggregate-root]
  (:version aggregate-root))


(spec/fdef timestamp
  :args
  (spec/cat :aggregate-root ::aggregate-root)
  :ret
  (spec/nilable ::timestamp))

(defn timestamp
  "Returns the last-updated timestamp for the aggregate root"
  [aggregate-root]
  (:timestamp aggregate-root))


(spec/fdef state
  :args
  (spec/cat :aggregate-root ::aggregate-root)
  :ret
  ::state)

(defn state
  "Returns the current state for the aggregate root"
  [aggregate-root]
  (:state aggregate-root))


(spec/fdef event-dispatcher
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event keyword?)
  :ret
  (spec/nilable ::type))

(defn event-dispatcher [aggregate-root event]
  (:type event))


;;TODO: find out how to spec multimethods
#_(spec/fdef handle-event
  :args
  (spec/cat :state ::aggregate-root?
            :event ::event?)
  :ret
  ::event?)

(defmulti handle-event
  "Dispatches an event to its specified handler. "
  event-dispatcher)

(defmethod handle-event :default [state event] state)


(spec/def ::aggregate-id ::id)

(spec/fdef next-version
  :args
  (spec/cat :aggregate-root ::aggregate-root)
  :ret
  (spec/keys
   :req-un
   [::aggregate-id
    ::version]))

(defn next-version
  "Returns the next expected version of the given aggregate. Returns 0 if the
  aggregate does not yet have a version."
  [aggregate-root]
  {:aggregate-id (:id aggregate-root)
   :version      (safe-inc (:version aggregate-root))})


(spec/fdef make-event
  :args
  (spec/cat :event-id ::event-id
            :timestamp ::timestamp
            :type ::type
            :data ::data)
  :ret
  ::event)

(defn make-event
  "Generates an event from the given input. event-id should be a valid id,
  timestamp is a long since epoch, type is a keyword and data is a map
  containing event-specific serializable data."
  [event-id timestamp type data]
  {:event-id  event-id
   :timestamp timestamp
   :type      type
   :data      data})


(spec/fdef apply-metadata-from
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event ::event)
  :ret
  (spec/and
   ::aggregate-root
   (spec/keys
    :req-un
    [::version
     ::timestamp])))

(defn- apply-metadata-from [aggregate-root event]
  (assoc aggregate-root
         :version (:version event)
         :timestamp (:timestamp event)))


(spec/fdef apply-state-from
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event ::event)
  :ret
  ::event)

(defn- apply-state-from [aggregate-root event]
  (update aggregate-root :state handle-event event))


(spec/fdef apply-change
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event ::event)
  :ret
  ::event)

(defn apply-change
  "Generates a new version of the aggregate root with the event applied."
  [aggregate-root event]
  (let [event-metadata  (next-version aggregate-root)
        versioned-event (merge event event-metadata)]
    (-> aggregate-root
      (apply-metadata-from versioned-event)
      (apply-state-from versioned-event)
      (update :uncommitted-events conj versioned-event))))


(spec/fdef replay-event
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event ::event)
  :ret
  ::aggregate-root)

(defn replay-event
  [aggregate-root event]
  (-> aggregate-root
    (apply-metadata-from event)
    (apply-state-from event)))


(spec/fdef materialize
  :args
  (spec/cat :events (spec/coll-of ::event)
            :type ::type)
  :ret
  any?)

(defn materialize [events type]
  (let [aggregate-id (:aggregate-id (first events))
        root         (aggregate aggregate-id type)]
    (reduce replay-event root events)))


(spec/fdef emit-event
  :args
  (spec/cat :aggregate-root ::aggregate-root
            :event-type ::type
            :event-data (spec/? any?))
  :ret
  ::event)

(defn emit-event
  ([aggregate-root event-type]
   (emit-event aggregate-root event-type {}))
  ([aggregate-root event-type event-data]
    (apply-change aggregate-root (make-event (random-id) (now) event-type event-data))))
