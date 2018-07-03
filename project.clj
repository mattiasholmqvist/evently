(defproject evently "0.1.2-SNAPSHOT"
  :description "Small event sourcing library for Clojure"
  :url "http://github.com/mattiasholmqvist/evently"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}}
  :deploy-repositories [["releases" {:url "https://clojars.org/evently" :creds :gpg}]])
