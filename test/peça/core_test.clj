(ns peça.core-test
  (:require [clojure.test :as t]
            [clojure.core.async :as a]
            [peça.core :as pc]
            [clojure.set :refer [subset?]]))

(defn check-handler
  "
  Given a wait group atom, returns a handler that asserts incoming message bering greater than current state, and updates it.
  Side-effectly updates wait group to signal test is finished.
  "
  [wg]
  (fn ([] -1)
    ([state msg] (t/is (< state msg))
                 (when (= 999 msg) (swap! wg not))
                 msg)))

(t/deftest forward-messages
  (let [continue (atom true)
        forward-handler (fn [target] (fn ([] :init) ([_ msg] (target msg) msg)))
        checker (pc/actor (check-handler continue) 1000)
        actor (pc/actor (forward-handler checker) 1000)]
    (mapv actor (range 1000))
    (while @continue (Thread/sleep 50))
    (t/is (zero? (count (filter #(%) [actor checker]))))
    (t/is (zero? (count (filter #(% :anything) [actor checker]))))))

(defn append-handler
  "
  Given a wait group atom and a result-set atom, returns a handler that asserts messages belonging to said set.
  Side-effectly updates wait group to signal test is finished.
  "
  [wg a-set]
  (fn ([] #{})
    ([state msg] (t/is (not (@a-set msg)))
                 (t/is (= state @a-set))
                 (swap! a-set conj msg)
                 (when (= 999 msg) (swap! wg not))
                 (conj state msg))))

(t/deftest loose-messages
  (let [result (atom #{})
        superset (into (sorted-set) (range 1000))
        continue (atom true)
        actor (pc/hasty-actor (append-handler continue result) 500)]
    (mapv actor superset)
    (while @continue (Thread/sleep 50))
    (t/is (subset? @result superset))
    (t/is (not (subset? superset  @result)))
    (t/is (< (count @result) (count superset)))
    (t/is (= 999 (apply max @result)))
    (t/is (nil? (actor)))
    (t/is (not (actor :anything)))))

(defn spawn-handler
  "
  Given a wait group atom and an initial state integer, returns a handler that recursively creates new actors and send them increasing messages, until stop condition is met.
  Side-effectly updates count wait group to signal test is finished.
  "
  [wg seed]
  (fn ([] seed)
    ([state msg] (t/is (= state msg))
                 (when (< state 999)
                   ((pc/actor (spawn-handler wg (inc state))) (inc state))
                   (swap! wg inc)))))

(t/deftest spawning
  (let [msgs (atom 0)
        primordial (pc/actor (spawn-handler msgs 0))]
    (primordial 0)
    (while (< @msgs 999) (Thread/sleep 50))
    (t/is (nil? (primordial)))
    (t/is (not (primordial :anything)))))

(defn pong-handler [back]
  (fn ([] false)
    ([state msg] #_(println "PONG - state:   " state)
                 #_(println "     - message: " msg)
                 (if (and state msg)
                   (back msg)
                   (back state)))))

(defn ping-handler [wg]
  (fn ([] false)
    ([state msg] #_(println "PING - state:   " state)
                 #_(println "     - message: " msg)
                 (cond
                   (not state) (let [pong (pc/actor (pong-handler msg))
                                     sent (pong false)]
                                 (t/is sent)
                                 [msg pong sent])
                   (vector? state) ((second state) msg)
                   (= state msg) (do (t/is (and state msg))
                                     (swap! wg not))))))

(t/deftest ping-pong
  (let [continue (atom true)
        ping (pc/actor (ping-handler continue))]
    (t/is (ping ping))
    (while @continue (Thread/sleep 50))
    (t/is (nil? (ping)))
    (t/is (not (ping :anything)))))

(t/deftest promises
  (let [continue (atom true)
        checker (pc/actor (check-handler continue) 1000)]
    (loop [p (promise)]
      (checker p)
      (when-not (and @continue (= @p 999))
        (t/is (checker (inc @p)))
        (recur (promise))))
    (t/is (nil? (checker)))
    (t/is (not (checker :anything)))))

(defn working-handler
  "
  A handler function that keeps sending incremented messages to a given actor, in this test, itself.
  "
  ([] 0)
  ([state msg] #_(println "WORKER - state: " state)
               #_(println "       - msg:   " msg)
               (cond
                 (vector? state) (let [[cb stt] state]
                                   (cb (inc stt))
                                   (a/<!! (a/timeout 100))
                                   [cb msg])
                 (number? state) (let [state' (inc state)]
                                   (msg state')
                                   [msg state']))))

(t/deftest worker
  (let [wrkr (pc/actor working-handler 10)]
    (wrkr wrkr)
    (loop [p (promise)]
      (wrkr p)
      (when (< (second @p) 10)
        (recur (promise))))
    (t/is (nil? (wrkr)))
    (t/is (not (wrkr :anything)))))
