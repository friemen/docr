(ns docr.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [compojure.core :as cp :refer [GET POST]]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [hiccup.util :as hiccup-util]
   [ring.util.response :as response]
   [docr.dispatch :as dispatch]
   [docr.timer :as timer]
   [docr.documents :as docs]
   [docr.frontend])
  (:use
   [clojure.tools.cli :only [cli]]
   [hiccup.middleware :only (wrap-base-url)]

   [ring.util.codec :only [url-decode]]
   [ring.adapter.jetty]))


;; Register actions and pages

(def action-map (dispatch/find-symbols "-action" 'docr.frontend))
(def page-map (dispatch/find-symbols "-page" 'docr.frontend))

;; Routing and Middleware

(def default-port 8081)
(def default-repo (io/file "/home/falko/Temp/Documents"))
(def default-base-path "")

(defn main-routes
  []
  (cp/routes
   (GET "/" []
        (response/redirect (str hiccup-util/*base-url* "/pages/home")))
   (GET "/download/:filename" [filename]
        (dispatch/provide (-> filename url-decode docs/find-by-filename :file)))
   (GET "/pages/:page" [page & params]
        (dispatch/render page-map page params))
   (POST "/actions/:action" [action & params]
         (dispatch/process action-map action params))
   (route/resources "/")
   (route/not-found "Unknown resource.")))


;; Schedule document updates to happen every 5 seconds

(defonce timer (timer/create docs/update-documents! 5))

;; Server start / stop

(defonce server (atom nil))

(defn stop []
  (when-let [s (deref server)]
    (timer/stop timer)
    (.stop s)
    (log/debug "Jetty stopped")
    (reset! server nil)))


(defn start []
  (stop)
  (println "Using repo directory" default-repo)
  (println "Using Jetty port" default-port)
  (println "Using URL base path" default-base-path)
  (let [app
        (-> (handler/site (main-routes))
            (wrap-base-url default-base-path))]
    (swap! server (fn [_] (run-jetty app {:port default-port :join? false}))))
  (log/debug "Jetty started")
  (alter-var-root #'docs/repo-dir (constantly default-repo))
  (timer/start timer)
  @server)

;; Start in standalone mode, load configuration (port, repo dir) from command line

(defn -main [& args]
  (let [[params _ usage]
        (cli args
             ["-p" "--port" "Port that Jetty binds itself to." :parse-fn #(Integer. %)]
             ["-d" "--repo" "Directory that the documents are stored in." :parse-fn io/file]
             ["-b" "--base-path" "URL base path" :parse-fn str])]
    (when-let [p (:port params)]
      (alter-var-root #'default-port (constantly p)))
    (when-let [d (:repo params)]
      (alter-var-root #'default-repo (constantly d)))
    (when-let [b (:base-path params)]
      (alter-var-root #'default-base-path (constantly b)))
    (start)))
