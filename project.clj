(defproject docr "1.0.2-SNAPSHOT"
  :description
  "My private document repo browser"

  :url
  "https://github.com/friemen/docr"

  :license
  {:name "Eclipse Public License"
   :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/tools.logging "0.5.0"]
   [org.clojure/tools.cli "0.4.2"]
   [log4j/log4j "1.2.17"]
   [ring/ring-core "1.8.0"]
   [ring/ring-jetty-adapter "1.8.0"]
   [compojure "1.6.1"]
   [lib-noir "0.9.9"]
   [hiccup "1.0.5"]
   [clj-time "0.15.2"]]

  :main
  docr.core

  :profiles
  {:uberjar
   {:aot :all}})
