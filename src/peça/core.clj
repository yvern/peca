(ns peça.core
  (:require [clojure.core.async :as a]))

(def ptype (type (promise)))
(defn promise? [p] (-> p type (= ptype)))

(defn gen-actor
  "
  Actor generator function. Expects buffer kind, handler function and total capacity, last defaulting to 1.
  Here, actors are comprised of a state, a buffered channel that receives and holds messages, and a handler funtion, called on messages.
  Buffers may be blocking, dropping or sliding (same from core.async).
  Handler functions must have 2 arities:
    0-arity: used to generate inial state
    2-arity: with state and message respectively, and the return value is the new state
  If the message is a `clojure.core/promise`, the handler function is not called. Instead, the current state is delivered to that promise.
  Returns a function, that called with one argument, sends that argument as message to the channel/actor, and with zero arguments closes the channel and stops the inner go-loop.
  "
  ([buffer-kind handler capacity]
   (let [mailbox (a/chan (buffer-kind capacity))]
     (a/go-loop [state (handler)]
       (when-some [msg (a/<! mailbox)]
         (recur (if (promise? msg)
                  (do (deliver msg state) state)
                  (handler state msg)))))
     (fn ([] (a/close! mailbox))
       ([msg] (a/put! mailbox msg)))))
  ([buffer-kind handler] (gen-actor buffer-kind handler 1)))

(def buffered-actor "Actor that blocks when full" (partial gen-actor a/buffer))
(def dropping-actor "Actor that drops new messages when full" (partial gen-actor a/dropping-buffer))
(def slidding-actor "Actor that drops old messages when full" (partial gen-actor a/sliding-buffer))

(def actor "Actor that blocks on full" buffered-actor)
(def busy-actor "Actor that drops new messages when full" dropping-actor)
(def hasty-actor "Actor that drops old messages when full" slidding-actor)
