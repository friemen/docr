(ns webtest.dispatch-test
  (:use [clojure.test]
        [webtest.dispatch]))

(defn no1-page [])

(deftest page-url-test
  (is (= "/pages/no1"
         (page-url #'no1-page))
  (is (= "/pages/no1?foo=123&bar=find%20me"
         (page-url #'no1-page {:foo 123 :bar "find me"})))))