(ns docr.timer
  (:use [clojure.tools.logging :only [debug]])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))


(defn create
  ([task]
     (create task 1))
  ([task seconds]
     {:executor (atom nil)
      :seconds seconds
      :task task}))


(defn start
  [t]
  (debug "Timer started" t)
  (reset! (:executor t) (ScheduledThreadPoolExecutor. 1))
  (.scheduleAtFixedRate @(:executor t) (:task t) 0 (:seconds t) TimeUnit/SECONDS))

(defn stop
  [t]
  (when-let [e @(:executor t)]
    (.shutdown e)
    (debug "Timer stopped" t)
    (reset! (:executor t) nil)))


;; sample usage: (def timer (create #(println "Hello")))