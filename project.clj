(defproject docr "1.0.1"
  :description "My private document repo browser"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.cli "0.2.2"]
                 [log4j/log4j "1.2.17"]
                 [ring/ring-core "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [compojure "1.1.3"]
                 [lib-noir "0.3.5"]
                 [hiccup "1.0.2"]
                 [clj-time "0.5.0"]]
  :plugins [[lein-ring "0.8.0"]]
  :ring {:handler docr.core/app}
  :main docr.core
  :repl-options { :port 9090 })
