(ns docr.utils
  (:require [clj-time.format :as dt])
  (:use [clj-time.core :only [now year]]))

(defn vslicer
  "Example: (vslicer first second) returns a function that transforms a collection like
   ([:A 1]
    [:B 2]
    [:C 3])
   to ([:A :B :C] [1 2 3])."
  [& fs]
  (fn [xs]
    (reduce (fn [xss x]
              (map #(conj %1 (%2 x)) xss fs))
            (repeat (count fs) [])
            xs)))

(def slice-pairs (vslicer first second))

(defn blank?
  [value]
  (or (nil? value)
      (and (string? value)
           (clojure.string/blank? value))))


(def ^:private date-formats (map dt/formatter
                                 ["dd.MM.yyyy" "yyyy-MM-dd" "MM/dd/yyyy" "yyyyMMdd"]))

(defn format-date
  [d]
  (if (nil? d) "" (dt/unparse (first date-formats) d)))

(defn current-year [] (year (now)))

(defn parse-date
  [s]
  (if (blank? s)
    nil
    (if (.endsWith s ".")
      (parse-date (str s (current-year)))
      (->> date-formats
           (map #(try (dt/parse % s) (catch Exception ex nil)))
           (drop-while nil?)
           first))))

