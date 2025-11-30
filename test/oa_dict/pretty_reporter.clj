(ns oa-dict.pretty-reporter
  (:require [clojure.test :as t]))

(defmethod t/report :fail [m]
  (t/with-test-out
    (println "FAIL:" (or (:message m) ""))
    (when-let [e (:expected m)]
      (println "  expected:" e))
    (when-let [a (:actual m)]
      (println "  actual:  " a))
    (when-let [o1 (:only-in-1 m)]
      (println "  only-in-1:" o1))
    (when-let [o2 (:only-in-2 m)]
      (println "  only-in-2:" o2))
    (when-let [mm (:mismatched m)]
      (println "  mismatched (k -> [v1 v2]):" mm))))

