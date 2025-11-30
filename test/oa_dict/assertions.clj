(ns oa-dict.assertions
  (:require [clojure.test :as t]
            [clojure.data :as data]
            [oa-dict.core :as dict]
            [clojure.set :as set]))

(defn dict-diff
  "only-in-1 — пары, которые есть только в первом словаре
   only-in-2 — пары, которые есть только во втором словаре
   mismatched — ключи, где значения отличаются"
  [d1 d2]
  (let [m1 (into {} (seq d1))
        m2 (into {} (seq d2))
        [only1 only2 _] (data/diff m1 m2)
        mismatched (into {}
                         (for [k (set/union (set (keys m1)) (set (keys m2)))
                               :let [v1 (get m1 k ::absent)
                                     v2 (get m2 k ::absent)]
                               :when (and (not= v1 ::absent)
                                          (not= v2 ::absent)
                                          (not= v1 v2))]
                           [k [v1 v2]]))]
    {:only-in-1 only1
     :only-in-2 only2
     :mismatched mismatched}))

(defmethod t/assert-expr 'dict/equals-dict?
  [msg form]
  (let [[_ d1 d2] form]
    `(let [v1# ~d1
           v2# ~d2
           ok?# (dict/equals-dict? v1# v2#)]
       (if ok?#
         (t/do-report {:type :pass
                       :message ~msg
                       :expected '~form
                       :actual true})
         (t/do-report (merge {:type :fail
                              :message ~msg
                              :expected '~form
                              :actual false}
                             (dict-diff v1# v2#))))
       ok?#)))

