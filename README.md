# peça

![Clojure CI](https://github.com/yvendruscolo/peca/workflows/Clojure%20CI/badge.svg?branch=master)


Probably the simplest actor-model-ish implementation for clojure you will find out there.

## Usage

In Clojure we have core.async, providing us with channels and go~~routines~~ blocks.
Even though Rich Hickey doesn't seem to be very fond of the actor model, it is still appropriate for many tasks, and may fit better in some people's logic.

That being said, this library builds upon core.async's beloved elements to build these simple, semi-actors. These are the key concepts:

* `state`: every actor starts off with a single value as its initial **state**, given by the 0-arity of the **handler function**.
* `handler function`: a function, with 2 arities, used to generate the actor's inital **state**, and the 2-arity used to update the **state** based on incoming **messages**, as the return value from it being called with both: `(handler-function state message)`; and to perform work/side-effects.
* there is a `go-loop` that reads from an inner, inaccecible channel/mailbox/queue, and if the message is a `promise`, then just `deliver`s the current state to that `promise`, otherwise feeds it to the **handler function**.
* the act/call to create/generate an actor, spawn our **channel** and `go-loop`, returning the only way of communicating with our newborn actor. But instead of a usual reference or pid, it returns a *function*, that called with no arguments, closes the **channel** and kills the `go-loop`, and with a single argument, sends that argument as message. Note, from the test/examples, that could even be a function returned from the creation of another actor, in order for both actors to interact.

## License

Copyright © 2020 Yuri Vendruscolo da Silveira

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
