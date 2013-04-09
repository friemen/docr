(ns docr.documents
  (:require [clj-time.format :as dt])
  (:use [clojure.tools.logging :only [debug]]
        [clojure.string :only [split upper-case]]
        [docr.utils]))

(defonce ^:pivate docs (atom []))

(defrecord Document [entity date subject file])

(defn documents []
  (->> @docs (sort #(.compareTo (:date %2) (:date %1)))))

(defn- value-contained?
  [text v]
  (or (blank? text)
      (and (not (nil? v))
           (.contains (upper-case v) (upper-case text)))))

(defn- value-in-range?
  [from to d]
  (and (or (nil? from) (<= (compare from d) 0))
       (or (nil? to) (>= (compare to d) 0))))

(defn matches?
  [params doc]
  (and (value-contained? (:entity params) (:entity doc))
       (value-contained? (:keyword params) (:subject doc))
       (value-in-range? (:date-from params) (:date-to params) (:date doc))))

(defn filter-by
  [params]
  (if (->> params vals (every? blank?))
    (documents)
    (filter (partial matches? params) (documents))))

(defn find-by-filename
  [filename]
  (->> @docs
       (filter #(= filename (.getName (:file %))))
       first))

;; An index represents an order and grouping of documents

(defprotocol Index
  (itype [this])
  (ititle [this])
  (iitem [this doc])
  (icomparator [this])
  (filter-by-index-key [this value docs]))

(def yyyy-MM (dt/formatter "yyyy-MM"))

(def index-types
  {:entity (reify Index
             (itype [_] "entity")
             (ititle [_] "Index by Entities")
             (iitem [_ doc] (:entity doc))
             (icomparator [_] #(compare %1 %2))
             (filter-by-index-key [_ key docs] (filter #(= key (:entity %)) docs)))
   :date (reify Index
           (itype [_] "date")
           (ititle [_] "Index by Months")
           (iitem [_ doc] (dt/unparse yyyy-MM (:date doc)))
           (icomparator [_] #(compare %2 %1))
           (filter-by-index-key [this key docs] (filter #(= key (iitem this %)) docs)))})


;; Reading directory for document files

(def yyyy-mm-dd (dt/formatter "yyyy-MM-dd"))
(def repo-dir "")

(defn- filename-without-ext
  [file]
  (let [name (.getName file)
        last-dot (.lastIndexOf name ".")]
    (if (> last-dot -1) (.substring name 0 last-dot) name)))

(defn- parse-filename
  [file]
  (let [name (filename-without-ext file)
        parts (split name #"__")]
    (when (= 3 (count parts))
      (let [[entity date subject] parts]
        (Document. entity (dt/parse yyyy-mm-dd date) subject file)))))

(defn- read-documents
  [dir]
  (->> dir
       file-seq
       (map parse-filename)
       (filter #(not (nil? %)))))

(defn update-documents!
  []
  (swap! docs (fn [_] (vec (read-documents repo-dir)))))