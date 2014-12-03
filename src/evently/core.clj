(ns evently.core)

(defn aggregate-id? "Returns true if the id is a valid aggregate id"
  [id]
  (string? id))

(defn aggregate-root?
  "Returns true if the id is a valid aggregate root"
  [ar]
  (and
    (aggregate-id? (:id ar))
    (string? (:type ar))
    (or
      (empty? (:uncommitted-events ar))
      (seq? (:uncommitted-events ar))))
    (pos? (:version ar))
    (associative? (:state ar)))

(defn aggregate
  "Creates an event-sourced aggregate root"
  [aggregate-id type]
  {:pre  [(string? aggregate-id)]
   :post  [(aggregate-root? %)]}
  {:id aggregate-id
   :type type
   :uncommitted-events []
   :version 1
   :state {}})

(defn events
  "Returns the uncommitted events of the aggregate root in insertion-order"
  [aggregate-root]
  {:pre  [(aggregate-root? aggregate-root)]}
  (:uncommitted-events aggregate-root))

(defn version
  "Returns the version of the aggregate root"
  [aggregate-root]
  {:pre  [(aggregate-root? aggregate-root)]}
  (:version aggregate-root))

(defn timestamp
  "Returns the last-updated timestamp for the aggregate root"
  [aggregate-root]
  {:pre  [(aggregate-root? aggregate-root)]}
  (:timestamp aggregate-root))

(defn state
  "Returns the current state for the aggregate root"
  [aggregate-root]
  {:pre  [(aggregate-root? aggregate-root)]}
  (:state aggregate-root))

(defn event-dispatcher [aggregate-root event]
  (:type event))

(defmulti handle-event
  "Dispatches an event to its specified handler. "
  event-dispatcher)

(defmethod handle-event :default [state event] state)

(defn event?
  "Returns true if the id is a valid event"
  [e]
  (and
    (string? (:event-id e))
    (pos? (:timestamp e))
    (keyword? (:type e))
    (associative? (:data e))))

(defn next-version
  "Returns the next expected version of the given aggregate"
  [aggregate-root]
  ((fnil inc 0) (:version aggregate-root)))

(defn make-event
  "Generates an event from the given input. event-id should be a valid id,
  timestamp is a long since epoch, type is a keyword and data is a map
  containing event-specific serializable data."
  [event-id timestamp type data]
  {:event-id event-id
   :timestamp timestamp
   :type type
   :data data})

(defn apply-change
  "Generates a new version of the aggregate root with the event applied."
  [aggregate-root event]
  {:pre  [(aggregate-root? aggregate-root)
          (event? event)]}
  (let [version (next-version aggregate-root)
        versioned-event (assoc event :version version)]
    (-> aggregate-root
      (assoc-in [:version] (:version versioned-event))
      (assoc-in  [:timestamp] (:timestamp versioned-event))
      (update-in [:uncommitted-events] conj event)
      (update-in [:state] handle-event versioned-event))))
