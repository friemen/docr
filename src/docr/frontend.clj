(ns docr.frontend
  (:require
   [hiccup.util :as hiccup-util]
   [docr.dispatch :as dispatch]
   [docr.documents :as docs])
  (:use
   [hiccup core page form]
   [docr.utils]))

;; Pages

(declare home-page index-page)

(defn- render-link
  ([label image page]
     (render-link label image page {}))
  ([label image page params]
  (list
   [:a {:href (dispatch/page-url page params)}
    [:img {:src (str hiccup-util/*base-url* image)} label]]
   [:span {:style "padding:30px"}])))

(defn frame
  [content]
  (html5 [:head [:title "DocR - Document Repository"] (include-css "/stylesheet.css")]
         [:body
          [:div {:class "PageHeader"}
           (render-link "DocR:" "/logo.png" #'home-page)
           (render-link "Search documents" "/find.png" #'home-page)
           (render-link "Index by Date" "/calendar.png" #'index-page {:type "date"})
           (render-link "Index by Entity" "/user.png" #'index-page {:type "entity"})]
          content]))

(defn- data-table-renderer
  [ & title-fn-pairs]
  (let [[titles fns] (slice-pairs title-fn-pairs)]
    (fn [xs]
      (if (empty? xs)
        "No data to display."
        [:table {:class "ResultTable"} (into [:tr ] (map #(vector :th %) titles))
         (->> xs
              (reduce (fn [[rows even-row?] x]
                        (let [cells (map #(vector :td (% x)) fns)
                              style (if even-row? "ResultTable-EvenRow" "ResultTable-OddRow")]
                          [(conj rows (into [:tr {:class style}] cells)) (not even-row?)]))
                      [[] true])
              first
              seq)]))))

(def ^:private render-documents
  (data-table-renderer
   ["Entity" (fn [doc]
               (let [value (:entity doc)]
                 [:a {:href (dispatch/page-url #'index-page
                                               {:type "entity" :key value})}
                  value]))]
   ["Date" (fn [doc] (-> doc :date format-date))]
   ["Subject" :subject]
   ["Action" (fn [doc]
               [:a
                {:href (str hiccup-util/*base-url* "/download/" (-> doc :file .getName))}
                "Open"])]))

(defn- render-form
  "Creates a html form for a vector of fields and buttons.
    - Title is a human readable text.
    - A field is a map {:key :label}.
    - A button is a map {:label}.
    - Params is a map with current form data."
  [title fields buttons params]
  (form-to [:post "/actions/apply-filter"]
           [:table {:class "Form"}
            [:tr [:td {:colspan 2} [:b title]]]
            (for [f fields]
              [:tr
               [:td (label (str (:label f) "-label") (:label f))]
               [:td (text-field (name (:key f)) (params (:key f)))]])
            [:tr [:td {:colspan 2 :align "right"}
                  (for [b buttons] (submit-button (:label b)))]]]))

(defn- make-button [label] {:label label})
(defn- make-field [label key] {:label label :key key})


(defn- parse-params
  [params]
  (assoc params :date-to (parse-date (:date-to params))
                :date-from (parse-date (:date-from params))))

(defn ^{:frame frame} home-page
  [params]
  (html [:p (render-form "Search for documents"
                     [(make-field "Entity" :entity)
                      (make-field "Date from" :date-from)
                      (make-field "Date to" :date-to)
                      (make-field "Keyword" :keyword)]
                     [(make-button "Find")]
                     params)]
        (let [ds (-> params
                     parse-params
                     docs/filter-by)]
          (list
           (if (->> [:entity :date-from :date-to :keyword]
                    (select-keys params)
                    (vals)
                    (str)
                    (every? blank?))
             [:p [:b (count ds) " documents"]]
             [:p [:b "Filter active, result shows " (count ds) " documents."]])
           (render-documents ds)))))

(defn- render-index
  [index]
  (let [items (->> (docs/documents)
                   (map #(docs/iitem index %)))
        freqs (frequencies items)]
    (->> items
         (sort (docs/icomparator index))
         (distinct)
         (map (fn [item]
                [:span {:style "margin:5px"}
                 [:a {:href (dispatch/page-url #'index-page {:type (docs/itype index) :key item})}
                  (if (> (freqs item) 5) [:font {:size 4} item] item)]]))
         (interpose " "))))

(defn ^{:frame frame} index-page
  [params]
  (let [index (get docs/index-types (keyword (:type params)))]
    (html [:p [:b (docs/ititle index)]]
          [:p (render-index index)]
          [:p (when-let [key (:key params)]
                (render-documents (->> (docs/documents) (docs/filter-by-index-key index key))))])))

;; Actions

(defn apply-filter-action
  [params]
  (dispatch/page-url #'home-page params))
