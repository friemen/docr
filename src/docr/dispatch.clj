(ns docr.dispatch
  "Provides infrastructure for a Post-Redirect-Get webapp that is structured
   into pages and actions. Use find-symbols to retrieve functions from namespaces
   whose names have a specific suffix (e.g. all *-pages and all *-actions
   functions).
   A page is a function that takes a map as parameters and returns a HTML string.
   An action is a function that takes a map as parameters and returns a redirect
   to a page URL as Ring response. Action function should use the page-url
   function to create the target URL.
   The functions provide, render and process can directly be used in a routing
   declaration. Render and process expect a page and action map, resp. as returned
   by find-symbols."
  (:require [hiccup.util :as hiccup-util]
            [ring.util.response :as response]
            [ring.util.codec :refer [url-encode]]
            [clojure.tools.logging :as log]))

;; Dispatching page/action names to functions

(defn without-suffix
  [s suffix]
  (if (.endsWith s suffix)
    (.substring s 0 (- (count s) (count suffix)))
    s))

(defn query-string [m]
  (->> (for [[k v] m]
         (str
          (url-encode (name k))
          "="
          (url-encode v)))
       (interpose "&")
       (apply str)))

(defn page-url
  ([sym]
     (page-url sym nil))
  ([sym param-map]
     (let [resource (str hiccup-util/*base-url* "/pages/" (without-suffix (-> sym meta :name str) "-page"))]
       (if-not (empty? param-map)
         (str resource "?" (query-string param-map))
         resource))))

(defn find-symbols
  [suffix ns]
  (->> ns
       ns-publics
       (filter (fn [[sym var]]
                 (.endsWith (str sym) suffix)))
       (map (fn [[sym var]]
              [(without-suffix (str sym) suffix) var]))
       (into {})))

(def mimetypes
  {"pdf"  "application/pdf"
   "txt"  "text/plain"
   "html" "text/html"})

(defn mimetype
  [filename]
  (let [ext (-> filename (clojure.string/split #"\u002E") last)]
    (mimetypes ext)))

(defn provide
  ([file]
   (provide (mimetype (.getName file)) file))
  ([mimetype file]
   (log/debug "Providing data for " file)
   {:status  200
    :headers {"mimetype" mimetype}
    :body    (clojure.java.io/input-stream file)}))

(defn render
  [page-map page params]
  (log/debug "Rendering" page params)
  (if-let [f (get page-map page)]
    (if-let [frame-fn (-> f meta :frame)]
      (frame-fn (f params))
      (f params))
    {:status 404
     :body   (str "Error: Page '" page "' is unknown")}))

(defn process
  [action-map action params]
  (log/debug "Processing Action" action params)
  (if-let [f (get action-map action)]
    (let [page (f params)]
      (response/redirect page))
    {:status 404
     :body   (str "Error: Action '" action "' is unknown")}))
