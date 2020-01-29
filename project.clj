(defproject peça "0.1.0-SNAPSHOT"
  :description "Probably the simplest actor-model-ish implementation for clojure you will find out there"
  :url "https://github.com/yvendruscolo/peca"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.7.559"]]
  :plugins [[lein-cljfmt "0.6.6"]]
  :repl-options {:init-ns peça.core})
