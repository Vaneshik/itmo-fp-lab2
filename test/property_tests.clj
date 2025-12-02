(ns property-tests
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [core :as dict]))

;; Генераторы
(def gen-kv-pair (gen/tuple gen/keyword (gen/choose 1 100)))
(def gen-pairs (gen/vector gen-kv-pair 1 10))

;; Добавление и получение
(defspec add-and-retrieve 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      [k v] (rand-nth pairs)]
                  (= v (dict/get-value d k)))))

;; Удаление
(defspec delete-removes-key 100
  (prop/for-all [pairs gen-pairs]
                (when (seq pairs)
                  (let [d (dict/dict-from pairs)
                        [k _] (rand-nth pairs)
                        d2 (dict/delete d k)]
                    (not (dict/contains-key? d2 k))))))

;; Количество элементов
(defspec count-matches-unique-keys 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      unique-keys (count (into {} pairs))]
                  (= unique-keys (count d)))))

;; Свойства моноида: идентичность
(defspec monoid-identity 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      empty dict/empty-dict]
                  (and (dict/equals-dict? d (dict/merge-dict empty d))
                       (dict/equals-dict? d (dict/merge-dict d empty))))))

;; Свойства моноида: ассоциативность
(defspec monoid-associativity 100
  (prop/for-all [p1 gen-pairs, p2 gen-pairs, p3 gen-pairs]
                (let [d1 (dict/dict-from p1)
                      d2 (dict/dict-from p2)
                      d3 (dict/dict-from p3)]
                  (dict/equals-dict?
                   (dict/merge-dict (dict/merge-dict d1 d2) d3)
                   (dict/merge-dict d1 (dict/merge-dict d2 d3))))))

;; Совместимость со стандартными Clojure map
(defspec clojure-map-compatibility 100
  (prop/for-all [pairs gen-pairs]
                (let [m (into {} pairs)
                      d (dict/dict-from pairs)]
                  (and (= (set m) (set (seq d)))
                       (= (count m) (count d))
                       (every? (fn [[k v]] (= v (get d k))) m)))))

;; ILookup: get согласован с get-value
(defspec lookup-consistent 100
  (prop/for-all [pairs gen-pairs
                 k gen/keyword]
                (let [d (dict/dict-from pairs)]
                  (= (get d k) (dict/get-value d k)))))

;; Associative: contains? согласован с contains-key?
(defspec contains-consistent 100
  (prop/for-all [pairs gen-pairs
                 k gen/keyword]
                (let [d (dict/dict-from pairs)]
                  (= (contains? d k) (dict/contains-key? d k)))))

;; Associative: assoc эквивалентен insert
(defspec assoc-equivalent-to-insert 100
  (prop/for-all [pairs gen-pairs
                 k gen/keyword
                 v (gen/choose 1 100)]
                (let [d (dict/dict-from pairs)
                      d-assoc (assoc d k v)
                      d-insert (dict/insert d k v)]
                  (dict/equals-dict? d-assoc d-insert))))

;; Seqable: seq содержит все пары
(defspec seq-contains-all-pairs 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      seq-pairs (set (seq d))
                      original-pairs (set (into {} pairs))]
                  (= seq-pairs original-pairs))))

;; Counted: count согласован с size seq
(defspec count-equals-seq-size 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)]
                  (= (count d) (count (seq d))))))

;; Filter сохраняет предикат
(defspec filter-preserves-predicate 100
  (prop/for-all [pairs gen-pairs]
                (let [pred (fn [[_ v]] (even? v))
                      d (dict/dict-from pairs)
                      filtered (dict/filter-dict d pred)]
                  (every? pred (dict/get-pairs filtered)))))

;; Map корректно преобразует значения
(defspec map-transforms-correctly 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      f (fn [[k v]] [k (* v 2)])
                      mapped (dict/map-dict d f)]
                  (every? (fn [[k v]]
                            (= (* v 2) (dict/get-value mapped k)))
                          (dict/get-pairs d)))))

;; Reduce работает корректно
(defspec reduce-works-correctly 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      f (fn [acc [_ v]] (+ acc v))
                      result-left (dict/reduce-left d f 0)
                      result-right (dict/reduce-right d f 0)
                      expected (reduce + 0 (vals (into {} pairs)))]
                  (and (= result-left expected)
                       (= result-right expected)))))

;; Равенство рефлексивно
(defspec equality-reflexive 100
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)]
                  (dict/equals-dict? d d))))

;; Равенство симметрично
(defspec equality-symmetric 100
  (prop/for-all [pairs gen-pairs]
                (let [d1 (dict/dict-from pairs)
                      d2 (dict/dict-from (reverse pairs))]
                  (= (dict/equals-dict? d1 d2)
                     (dict/equals-dict? d2 d1)))))

;; Bind: правая идентичность монады
(defspec bind-monad-right-identity 50
  (prop/for-all [pairs gen-pairs]
                (let [d (dict/dict-from pairs)
                      return-fn (fn [k v] (dict/insert dict/empty-dict k v))]
                  (dict/equals-dict? d (dict/bind-dict d return-fn)))))

