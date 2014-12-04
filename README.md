![](https://travis-ci.org/mattiasholmqvist/evently.svg?branch=master)

# evently

Event sourcing example for Clojure.

*This project is under heavy development. Use at your own risk!*

## Usage

### Rationale
evently uses Clojure data structures to represent aggregates and events. This makes for a simple API that can make use of the Clojure core library functions.

### Creating an aggregate root

```clojure
(use 'evently.core)
(use 'evently.utils)

(aggregate (random-id) :order)
```
### Emitting events
To handle the event in the aggregate root and allow for update of the internal state, evently provides a multimethod `handle-event`which should be extended with the event type. The multimethod is invoked with an aggregate state map and the event map, which allows for modifying the aggregate root.

*Note: It is important to only update the state of the aggregate root and **not** implement domain logic in * `handle-event`. This will break the materialization of aggregate roots from previously stored events.

In the example below we update the order aggregate status to `:new-status`:
```clojure
(defmethod handle-event :something-happened [state event]
  (assoc state :status :new-status))
  ```
The aggregate root can now emit an event (using `emit-event`) and evently will call the multimethod that matches the event type.

```clojure
(let [order (aggregate (random-id) :order)]
  (-> order
      (emit-event :something-happened)))
```

### Materializing aggregate roots
When loading an event-sourced aggregate root we need to materialize it from all historically saved events.

As an example we can use a vector of two events to materialize an aggregate root of type `:order`:
```clojure
(let [events [{:version 1, :aggregate-id "eb5b986d-c2d5-46cf-bfed-de80dd63e9e4", :event-id "f04bef23-54e0-448a-a846-f15caaedd2c0", :timestamp 1417695223489, :type :something, :data {}}
{:version 2, :aggregate-id "eb5b986d-c2d5-46cf-bfed-de80dd63e9e4", :event-id "7893e677-837b-4908-b1c3-24f3415f1849", :timestamp 1417695223490, :type :something-else, :data {}}]]
  (materialize events :order))
```
If we now have extensions to the multimethod `handle-event`, these will be called in sequence when the events are processed. This will result in a materialized aggregate root with no uncommitted events.

### Chaining functions
You may have noticed that the API supports chaining functions using the `->` operator. This is due to that all the public functions in evently uses the aggregate root as their first argument.

As a principle, you should also try to make that the first argument of your functions since it would make chaining your functions with evently functions simple and nice. See the [test code](https://github.com/mattiasholmqvist/evently/blob/master/test/evently/order_test.clj) for examples of this!

### I need more examples
For a more extensive example see [order_test.clj](https://github.com/mattiasholmqvist/evently/blob/master/test/evently/order_test.clj) which implements an order aggregate root with some domain logic and validation code.

## License

Copyright © 2014 Mattias Holmqvist

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
