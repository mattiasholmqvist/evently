![](https://travis-ci.org/mattiasholmqvist/evently.svg?branch=master)

# evently

Event sourcing library for Clojure.

*This project is under heavy development. Use at your own risk!*

*Please give feedback and send pull requests!*

## Usage

### Rationale
evently uses Clojure data structures to represent aggregates and events. This makes for a simple API that can make use of the Clojure core library functions.

### Creating an aggregate root

```clojure
(use 'evently.core)
(use 'evently.utils)

(aggregate (random-id) :order)

; Result
{:id "0a47533e-5f0b-4a2f-8981-ae8273dbfdd6", :type :order, :uncommitted-events [], :version 0, :state {}}
```
### Emitting events
To handle events in the aggregate root which allows for updating the internal state, evently provides a multimethod `handle-event` which is extended with the event type. The multimethod is invoked with an aggregate state map and the event map. This function should return the updated state of the aggregate.

*Note: It is important to only update the state of the aggregate root and **not** implement domain logic in * `handle-event`. This will break the materialization of aggregate roots from previously stored events.

In the example below we update the order aggregate status to `:new-status`:
```clojure
(defmethod handle-event :something-happened [state event]
  (assoc state :status :new-status))
  ```
The aggregate root can emit events (using `emit-event`) as a result from different business rules. evently will add the event to the list of uncommitted events inside the aggregate root and call the `handle-event` multimethod that matches the event type.

```clojure
(let [order (aggregate (random-id) :order)]
  (-> order
      (emit-event :something-happened)))
```
If you want to attach data to the emitted event you can supply a data map as the last argument to `emit-event`:
```clojure
(let [order (aggregate (random-id) :order)]
(-> order
  (emit-event :something-happened {:important-event-data 123})))
```

### Storing events
To get the list of uncommitted events from an aggregate root you can use the `events` function. This returns a pure Clojure vector of maps, which can be stored however you like:

```clojure
(let [order (aggregate (random-id) :order)]
(-> order
  (emit-event :something-happened)
  events))

; Result
[{:version 1,
  :aggregate-id "674d887d-4057-411f-ac5e-bb87ba746646",
  :event-id "9282ae08-cf9e-45d3-9818-3136c3aff141",
  :timestamp 1417703765661,
  :type :something-happened,
  :data {}}]
```

### Materializing aggregate roots
When loading an event-sourced aggregate root we need to materialize it from all historically saved events.

As an example we can use a vector of two events to materialize an aggregate root of type `:order`:
```clojure
(let [events [
  {:version 1,
   :aggregate-id "eb5b986d-c2d5-46cf-bfed-de80dd63e9e4",
   :event-id "f04bef23-54e0-448a-a846-f15caaedd2c0",
   :timestamp 1417695223489,
   :type :something,
   :data {}
  }
  {:version 2,
    :aggregate-id "eb5b986d-c2d5-46cf-bfed-de80dd63e9e4",
    :event-id "7893e677-837b-4908-b1c3-24f3415f1849",
    :timestamp 1417695223490,
    :type
    :something-else, :data {}
  }]]
  (materialize events :order))
```
If we now have extensions to the multimethod `handle-event`, these will be called in sequence when the events are processed. This will result in a materialized aggregate root with no uncommitted events.

### Chaining functions
You may have noticed that the API supports chaining functions using the `->` operator. This is due to that all the public functions in evently uses the aggregate root as their first argument.

As a principle, you should also try to make that the first argument of your functions since it would make chaining your functions with evently functions simple and nice. See the [test code](https://github.com/mattiasholmqvist/evently/blob/master/test/evently/order_test.clj) for examples of this!

### More examples
For a more extensive example see [order_test.clj](https://github.com/mattiasholmqvist/evently/blob/master/test/evently/order_test.clj) which implements an order aggregate root with some domain logic and validation code.

## License

Copyright © 2014 Mattias Holmqvist

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
