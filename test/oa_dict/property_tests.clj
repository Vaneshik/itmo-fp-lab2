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

;; Композитные генераторы с использованием gen/let

(def gen-dict
  "Генератор словаря из случайных пар"
  (gen/let [pairs gen-pairs]
    (dict/dict-from pairs)))

(def gen-non-empty-dict
  "Генератор непустого словаря (гарантированно ≥1 элемент)"
  (gen/let [first-pair (gen/tuple gen-key gen-value)
            rest-pairs gen-pairs]
    (dict/dict-from (cons first-pair rest-pairs))))

(def gen-dict-with-known-key
  "Генератор словаря с известным ключом для тестирования операций поиска"
  (gen/let [known-key gen-key
            known-val gen-value
            other-pairs gen-pairs]
    {:dict (dict/insert (dict/dict-from other-pairs) known-key known-val)
     :key known-key
     :value known-val}))

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

;; Property-based тесты для bind

(defspec property-bind-preserves-structure ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      result (dict/bind-dict d (fn [k v] (dict/insert (dict/empty-dict) k v)))]
                  (dict/equals-dict? d result))))

(defspec property-bind-monad-left-identity ITERATIONS
  (prop/for-all [k gen-key
                 v gen-value]
                (let [d (dict/insert (dict/empty-dict) k v)
                      f (fn [key val] (dict/insert (dict/empty-dict) key (* val 2)))
                      left (dict/bind-dict d f)
                      right (f k v)]
                  (dict/equals-dict? left right))))

(defspec property-bind-monad-right-identity ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      result (dict/bind-dict d (fn [k v] (dict/insert (dict/empty-dict) k v)))]
                  (dict/equals-dict? d result))))

(defspec property-bind-combines-results ITERATIONS
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      result (dict/bind-dict d
                                             (fn [k v]
                                               (-> (dict/empty-dict)
                                                   (dict/insert k v)
                                                   (dict/insert :sum (+ v 100)))))]
                  ;; Проверяем что все ключи присутствуют в результате
                  (every? #(dict/contains-key? result %) (dict/get-keys d)))))

