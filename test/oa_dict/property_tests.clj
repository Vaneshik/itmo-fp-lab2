(ns oa-dict.property-tests
  {:clj-kondo/config '{:lint-as {clojure.test.check.clojure-test/defspec clojure.core/def
                                 clojure.test.check.properties/for-all clojure.core/let}}}
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [oa-dict.core :as dict]))

(def MIN_VAL 1)
(def MAX_VAL 200)
(def ITERATIONS 100)
(def MAX_SIZE 10)

(def gen-value (gen/choose MIN_VAL MAX_VAL))
(def gen-key gen/keyword)

(def gen-pairs (gen/vector (gen/tuple gen-key gen-value) 1 MAX_SIZE))

(defspec property-contains-inserted-elements ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      [k _] (rand-nth pairs)]
                  (dict/contains-key? d k))))

(defspec property-merge-preserves-keys ITERATIONS
  (prop/for-all [pairs1 gen-pairs
                 pairs2 gen-pairs]
                (let [d1 (dict/dict-from pairs1)
                      d2 (dict/dict-from pairs2)
                      merged-d (dict/merge-dict d1 d2)
                      merged-pairs (concat pairs1 pairs2)]
                  (every? (fn [[k _]] (dict/contains-key? merged-d k)) merged-pairs))))

(defspec property-monoid-identity ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)]
                  (and (dict/equals-dict? d (dict/merge-dict (dict/empty-dict) d))
                       (dict/equals-dict? d (dict/merge-dict d (dict/empty-dict)))))))

(defspec property-monoid-associativity ITERATIONS
  (prop/for-all [pairs1 gen-pairs
                 pairs2 gen-pairs
                 pairs3 gen-pairs]
                (let [d1 (dict/dict-from pairs1)
                      d2 (dict/dict-from pairs2)
                      d3 (dict/dict-from pairs3)]
                  (dict/equals-dict?
                   (dict/merge-dict (dict/merge-dict d1 d2) d3)
                   (dict/merge-dict d1 (dict/merge-dict d2 d3))))))

(defspec property-ilookup-compatibility ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [m (into {} pairs)
                      d (dict/dict-from pairs)
                      ks (into (set (keys m)) #{::missing-1 ::missing-2})]
                  (every?
                   true?
                   (for [k ks]
                     (= (get m k ::nf) (get d k ::nf)))))))

(defspec property-associative-interface ITERATIONS
  (prop/for-all [pairs gen-pairs
                 k gen-key
                 v gen-value]
                (let [m (assoc (into {} pairs) k v)
                      d (assoc (dict/dict-from pairs) k v)]
                  (and
                   (every?
                    true?
                    (for [kk (conj (keys m) ::missing)]
                      (= (contains? m kk) (contains? d kk))))
                   (let [em (find m k)
                         ed (find d k)]
                     (or (and (nil? em) (nil? ed))
                         (= (seq em) (seq ed))))))))

(defspec property-seqable-counted ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [m (into {} pairs)
                      d (dict/dict-from pairs)]
                  (and
                   (= (set m) (set (seq d)))
                   (= (count m) (count d))))))

