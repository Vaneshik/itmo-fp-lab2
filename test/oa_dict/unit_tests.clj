(ns oa-dict.unit-tests
  (:require
   [clojure.test :refer [deftest testing is]]
   [oa-dict.core :as dict]))

(deftest basic-operations-test
  (testing "Базовые операции insert/get/delete"
    (let [d (dict/empty-dict)
          d1 (dict/insert d :a 1)
          d2 (dict/delete d1 :a)]
      (is (nil? (dict/get-value d :a)))
      (is (= 1 (dict/get-value d1 :a)))
      (is (nil? (dict/get-value d2 :a))))))

(deftest batch-insert-test
  (testing "Вставка массива пар"
    (let [d (-> (dict/empty-dict)
                (dict/insert-all [[:a 1] [:b 2] [:c 3]]))]
      (is (= 1 (dict/get-value d :a)))
      (is (= 2 (dict/get-value d :b)))
      (is (= 3 (dict/get-value d :c))))))

(deftest keys-values-pairs-extraction-test
  (testing "Получение ключей, значений, пар"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))]
      (is (= #{:a :b} (set (dict/get-keys d))))
      (is (= #{1 2} (set (dict/get-values d))))
      (is (= #{[:a 1] [:b 2]} (set (dict/get-pairs d)))))))

(deftest deletion-test
  (testing "Удаление по существующему ключу"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/delete :a))]
      (is (nil? (dict/get-value d :a)))))
  (testing "Удаление по не существующему ключу"
    (let [d (-> (dict/empty-dict)
                (dict/delete :a))]
      (is (nil? (dict/get-value d :a))))))

(deftest filtering-test
  (testing "Фильтрация"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2)
                (dict/insert :c 3)
                (dict/filter-dict (fn [[_ v]] (> v 1))))]
      (is (nil? (dict/get-value d :a)))
      (is (= 2 (dict/get-value d :b)))
      (is (= 3 (dict/get-value d :c))))))

(deftest mapping-test
  (testing "Мап"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2)
                (dict/map-dict (fn [[k v]] [k (* v 10)])))]
      (is (= 10 (dict/get-value d :a)))
      (is (= 20 (dict/get-value d :b))))))

(deftest reduce-operations-test
  (testing "Левая свёртка"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2)
                (dict/insert :c 3)
                (dict/reduce-left (fn [a [_ v]] (+ a v)) 0))]
      (is (= 6 d))))
  (testing "Правая свёртка"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2)
                (dict/insert :c 3)
                (dict/reduce-right (fn [a [_ v]] (+ a v)) 0))]
      (is (= 6 d)))))

(deftest equality-check-test
  (testing "Равные словари"
    (let [d1 (-> (dict/empty-dict)
                 (dict/insert :a 1)
                 (dict/insert :b 2))
          d2 (-> (dict/empty-dict)
                 (dict/insert :b 2)
                 (dict/insert :a 1))]
      (is (dict/equals-dict? d1 d2))))

  (testing "Не равные словари"
    (let [d1 (-> (dict/empty-dict)
                 (dict/insert :a 1)
                 (dict/insert :b 2))
          d2 (-> (dict/empty-dict)
                 (dict/insert :b 3)
                 (dict/insert :a 1))]
      (is (not (dict/equals-dict? d1 d2))))))

(deftest merge-operation-test
  (testing "Слияние словарей"
    (let [d1 (-> (dict/empty-dict)
                 (dict/insert :a 1))
          d2 (-> (dict/empty-dict)
                 (dict/insert :b 2))
          merged (dict/merge-dict d1 d2)]
      (is (= 1 (dict/get-value merged :a)))
      (is (= 2 (dict/get-value merged :b))))))

(deftest empty-dict-test
  (testing "Пустой словарь"
    (let [d (dict/empty-dict)]
      (is (nil? (dict/get-value d :nonexistent)))
      (is (empty? (dict/get-keys d)))
      (is (empty? (dict/get-values d)))
      (is (empty? (dict/get-pairs d))))))

(deftest monoid-identity-law-test
  (testing "Единичный элемент в моноиде"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))]
      (is (dict/equals-dict? d (dict/merge-dict (dict/empty-dict) d)))
      (is (dict/equals-dict? d (dict/merge-dict d (dict/empty-dict)))))))

(deftest monoid-associativity-law-test
  (testing "Ассоциативность слияния"
    (let [d1 (-> (dict/empty-dict) (dict/insert :a 1))
          d2 (-> (dict/empty-dict) (dict/insert :b 2))
          d3 (-> (dict/empty-dict) (dict/insert :c 3))]
      (is (dict/equals-dict?
           (dict/merge-dict (dict/merge-dict d1 d2) d3)
           (dict/merge-dict d1 (dict/merge-dict d2 d3)))))))

(deftest ilookup-interface-test
  (testing "Тестирование ILookup: get(m k) | get(m k not-found)"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))]
      (is (= 1 (get d :a)))
      (is (= 2 (get d :b)))
      (is (= 3 (get d :c 3)))
      (is (nil? (get d :c))))))

(deftest associative-interface-test
  (testing "Тестирование Associative: assoc | contains? | find"
    (let [d (-> (dict/empty-dict)
                (assoc :a 1)
                (assoc :b 2))
          e (find d :a)]
      (is (= 1 (get d :a)))
      (is (= 2 (get d :b)))
      (is (true?  (contains? d :a)))
      (is (false? (contains? d :c)))
      (is (instance? clojure.lang.IMapEntry e))
      (is (= [:a 1] e)))))

(deftest persistent-map-interface-test
  (testing "Тестирование IPersistentMap: dissoc | without"
    (let [d1  (-> (dict/empty-dict) (assoc :a 1) (assoc :b 2))
          d2 (dissoc d1 :a)]
      (is (nil? (get d2 :a)))
      (is (= 2 (get d2 :b)))
      (is (= 1 (get d1 :a))))))

(deftest seqable-interface-test
  (testing "Тестирование Seqable: seq"
    (let [d (-> (dict/empty-dict) (assoc :a 1) (assoc :b 2))
          s (seq d)]
      (is (seq s))
      (is (every? #(instance? clojure.lang.IMapEntry %) s))
      (is (= #{[:a 1] [:b 2]} (set s))))))

(deftest counted-interface-test
  (testing "Тестирование Counted: count"
    (let [d (-> (dict/empty-dict) (assoc :a 1) (assoc :b 2) (assoc :c 3))]
      (is (= 3 (count d)))
      (is (= 0 (count (dict/empty-dict)))))))

(deftest persistent-collection-test
  (testing "Тестирование IPersistentCollection: conj с вектором"
    (let [d1  (-> (dict/empty-dict) (assoc :a 1))
          d2 (conj d1 [:b 2])]
      (is (= 2 (get d2 :b)))))
  (testing "Тестирование IPersistentCollection: conj с map"
    (let [d1  (-> (dict/empty-dict) (assoc :a 1))
          d2 (conj d1 {:b 2 :c 3})]
      (is (= 1 (get d2 :a)))
      (is (= 2 (get d2 :b)))
      (is (= 3 (get d2 :c)))))
  (testing "Тестирование IPersistentCollection: into с последовательностью пар"
    (let [d1  (dict/empty-dict)
          d2 (into d1 [[:a 1] [:b 2]])]
      (is (= 1 (get d2 :a)))
      (is (= 2 (get d2 :b))))))

(deftest bind-operation-test
  (testing "bind на пустом словаре"
    (let [result (dict/bind-dict (dict/empty-dict)
                                 (fn [k v] (dict/insert (dict/empty-dict) k (* v 2))))]
      (is (= 0 (count result)))))

  (testing "bind с функцией, возвращающей пустой словарь"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))
          result (dict/bind-dict d (fn [_ _] (dict/empty-dict)))]
      (is (= 0 (count result)))))

  (testing "bind с функцией, создающей новые пары"
    (let [d (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))
          result (dict/bind-dict d
                                 (fn [k v]
                                   (-> (dict/empty-dict)
                                       (dict/insert k v)
                                       (dict/insert (keyword (str (name k) "-doubled")) (* v 2)))))]
      (is (= 1 (dict/get-value result :a)))
      (is (= 2 (dict/get-value result :a-doubled)))
      (is (= 2 (dict/get-value result :b)))
      (is (= 4 (dict/get-value result :b-doubled)))))

  (testing "bind объединяет результаты через merge"
    (let [d (-> (dict/empty-dict)
                (dict/insert :x 10))
          result (dict/bind-dict d
                                 (fn [k v]
                                   (-> (dict/empty-dict)
                                       (dict/insert :result v)
                                       (dict/insert :original k))))]
      (is (= 10 (dict/get-value result :result)))
      (is (= :x (dict/get-value result :original))))))

(deftest bind-monad-laws-test
  (testing "Левая идентичность монады: bind(return(x), f) = f(x)"
    (let [x 42
          f (fn [_ v] (dict/insert (dict/empty-dict) :result (* v 2)))
          d (dict/insert (dict/empty-dict) :test x)
          left (dict/bind-dict d f)
          right (f :test x)]
      (is (dict/equals-dict? left right))))

  (testing "Правая идентичность монады: bind(m, return) = m"
    (let [m (-> (dict/empty-dict)
                (dict/insert :a 1)
                (dict/insert :b 2))
          result (dict/bind-dict m (fn [k v] (dict/insert (dict/empty-dict) k v)))]
      (is (dict/equals-dict? m result))))

  (testing "Ассоциативность монады: bind(bind(m, f), g) = bind(m, λx.bind(f(x), g))"
    (let [m (-> (dict/empty-dict)
                (dict/insert :x 5))
          f (fn [k v] (dict/insert (dict/empty-dict) k (* v 2)))
          g (fn [k v] (dict/insert (dict/empty-dict) k (+ v 10)))
          left (dict/bind-dict (dict/bind-dict m f) g)
          right (dict/bind-dict m (fn [k v] (dict/bind-dict (f k v) g)))]
      (is (dict/equals-dict? left right)))))

