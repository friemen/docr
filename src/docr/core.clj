(ns docr.core
  (:gen-class)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [docr.dispatch :as dispatch]
            [docr.timer :as timer]
            [docr.documents :as docs]
            [docr.frontend])
  (:use [clojure.tools.logging :only [debug]]
        [clojure.tools.cli :only [cli]]
        [hiccup.middleware :only (wrap-base-url)]
        [compojure.core]
        [ring.util.codec :only [url-decode]]
        [ring.util.response]
        [ring.adapter.jetty]))


;; Register actions and pages

(def action-map (dispatch/find-symbols "-action" 'docr.frontend))
(def page-map (dispatch/find-symbols "-page" 'docr.frontend))

;; Routing and Middleware

(defroutes main-routes
  (GET "/" []
       (redirect "/pages/home"))
  (GET "/download/:filename" [filename]
       (dispatch/provide (-> filename url-decode docs/find-by-filename :file)))
  (GET "/pages/:page" [page & params]
       (dispatch/render page-map page params))
  (POST "/actions/:action" [action & params]
        (dispatch/process action-map action params))
  (route/resources "/")
  (route/not-found "Unknown resource."))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))


;; Schedule document updates to happen every 5 seconds

(defonce timer (timer/create docs/update-documents! 5))

;; Server start / stop

(def default-port 8081)
(def default-repo (clojure.java.io/file "/home/riemensc/Documents/Archive/Filed"))

(defonce server (atom nil))

(defn stop []
  (when-let [s (deref server)]
    (timer/stop timer)
    (.stop s)
    (debug "Jetty stopped")
    (reset! server nil)))


(defn start []
  (stop)
  (println "Using repo directory" default-repo)
  (println "Using Jetty port" default-port)
  (swap! server (fn [_] (run-jetty app {:port default-port :join? false})))
  (debug "Jetty started")
  (alter-var-root #'docs/repo-dir (fn [_] default-repo))
  (timer/start timer)
  @server)

;; Start in standalone mode, load configuration (port, repo dir) from command line

(defn -main [& args]
  (let [[params _ usage]
        (cli args
             ["-p" "--port" "Port that Jetty binds itself to." :parse-fn #(Integer. %)]
             ["-d" "--repo" "Directory that the documents are stored in." :parse-fn clojure.java.io/file])]
    (when-let [p (:port params)]
      (alter-var-root #'default-port (fn [_] p)))
    (when-let [d (:repo params)]
      (alter-var-root #'default-repo (fn [_] d)))
    (start)))