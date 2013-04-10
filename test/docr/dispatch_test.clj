(ns docr.dispatch-test
  (:use [clojure.test]
        [docr.dispatch]))

;; Test pages and actions

(defn frame [s] (str "TOP " s " BOTTOM"))
(defn no1-page [params] "Content 1")
(defn ^{:frame frame} no2-page [params] "Content 2")

(defn doit-action [params] (page-url #'no1-page))

;; Tests

(deftest without-suffix-test
  (are [result s suffix] (= result (without-suffix s suffix))
       "foo" "foo-bar" "-bar"
       ""    "-bar"    "-bar"
       "foo" "foo"     "-bar"))

(deftest query-string-test
  (are [result           parammap] (= result (query-string parammap))
       ""                {}
       "foo=ba%20r"      {:foo "ba r"}
       "foo=f&bar=b"     {:foo "f" :bar "b"}))

(deftest find-symbols-test
  (is (= {"no1" #'no1-page
          "no2" #'no2-page}
         (find-symbols "-page" 'docr.dispatch-test))))

(deftest page-url-test
  (is (= "/pages/no1"
         (page-url #'no1-page))
  (is (= "/pages/no1?foo=123&bar=find%20me"
         (page-url #'no1-page {:foo 123 :bar "find me"})))))

(deftest mimetype-test
  (are [result filename] (= result (mimetype filename))
       "application/pdf" "foo.bar.pdf"
       nil               "foo.bar"))

(deftest provide-test
  (with-redefs [clojure.java.io/input-stream (fn [file] "stream")]
    (is (= {:status 200 :headers {"mimetype" "text/plain"} :body "stream"}
           (provide (clojure.java.io/file "foo.txt"))))))

(deftest render-test
  (let [pages (find-symbols "-page" 'docr.dispatch-test)]
    (is (= 404 (:status (render pages "foo" {}))))
    (are [result                page] (= result (render pages page {}))
         "Content 1"            "no1"
         "TOP Content 2 BOTTOM" "no2")))

(deftest process-test
  (let [actions (find-symbols "-action" 'docr.dispatch-test)]
    (is (= 404 (:status (process actions "foo" {}))))
    (is (= {:status 302, :headers {"Location" "/pages/no1"}, :body ""}
           (process actions "doit" {})))))