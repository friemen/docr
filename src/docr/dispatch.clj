(ns docr.dispatch
  (:use [ring.util.response]
        [ring.util.codec :only [url-encode]]
        [clojure.tools.logging :only [debug]]))

;; Dispatching page/action names to functions

(defn without-suffix
  [s suffix]
  (if (.endsWith s suffix)
    (.substring s 0 (- (count s) (count suffix)))
    s))

(defn query-string [m]
  (->> (for [[k v] m]
         (str
          (url-encode (if (keyword? k) (.substring (str k) 1)  k))
          "="
          (url-encode v)))
       (interpose "&")
       (apply str)))

(defn page-url
  ([sym]
     (page-url sym nil))
  ([sym param-map]
     (let [resource (str "/pages/" (without-suffix (-> sym meta :name str) "-page"))]
       (if-not (empty? param-map)
         (str resource "?" (query-string param-map))
         resource))))

(defn find-symbols
  [ns suffix]
  (into {} (map (fn [[sym var]]
                  [(without-suffix (str sym) suffix) var])
                (filter (fn [[sym var]]
                          (.endsWith (str sym) suffix))
                        (ns-publics ns)))))

(def mimetypes {"pdf"  "application/pdf"
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
  (debug "Providing data for " file)
  {:status 200 :headers {"mimetype" mimetype} :body (clojure.java.io/input-stream file)}))

(defn render
  [page-map page params]
  (debug "Rendering" page params)
  (if-let [fn (get page-map page)]
    (if-let [frame-fn (-> fn meta :frame)]
      (frame-fn (fn params))
      (fn params))
    {:status 404 :body (str "Error: Page '" page "' is unknown")}))

(defn process
  [action-map action params]
  (debug "Processing Action" action params)
  (if-let [fn (get action-map action)]
    (let [page (fn params)]
      (redirect page))
    {:status 404 :body (str "Error: Action '" action "' is unknown")}))

