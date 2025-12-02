(ns impl
  (:require [protocol :as p]
            [utils :as u]))

(deftype OADict [table]
  p/Dict

  (insert [_ key value]
    (let [idx (mod (hash key) (count table))
          slot-idx (u/locate-empty-cell table key idx)]
      (if slot-idx
        (OADict. (assoc table slot-idx [key value]))
        (throw (ex-info "HashMap is full" {:key key :value value})))))

  (insert-all [dict pairs]
    (reduce (fn ins [d [k v]] (p/insert d k v)) dict pairs))

  (get-value [_ key]
    (let [idx (mod (hash key) (count table))
          slot-idx (u/search-key table key idx)]
      (when slot-idx
        (let [slot (nth table slot-idx)]
          (when (vector? slot)
            (second slot))))))

  (contains-key? [_ key]
    (let [idx (mod (hash key) (count table))
          slot-idx (u/search-key table key idx)]
      (boolean slot-idx)))

  (get-keys [dict]
    (map first (p/get-pairs dict)))

  (get-values [dict]
    (map second (p/get-pairs dict)))

  (get-pairs [_]
    (filter vector? table))

  (delete [_ key]
    (let [idx (mod (hash key) (count table))
          slot-idx (u/search-key table key idx)]
      (if slot-idx
        (OADict. (assoc table slot-idx :deleted))
        (OADict. table))))

  (filter-dict [this pred]
    (->> (p/get-pairs this)
         (filter pred)
         (p/insert-all (OADict. (vec (repeat u/TABLE_SIZE nil))))))

  (map-dict [this f]
    (->> (p/get-pairs this)
         (map f)
         (p/insert-all (OADict. (vec (repeat u/TABLE_SIZE nil))))))

  (reduce-left [this f init]
    (->> (p/get-pairs this)
         (reduce f init)))

  (reduce-right [this f init]
    (->> (p/get-pairs this)
         (u/my-reduce-right f init)))

  (equals-dict? [dict1 dict2]
    (let [p1 (p/get-pairs dict1)
          p2 (p/get-pairs dict2)]
      (and
       (reduce (fn f [acc [k v]] (and acc (= (p/get-value dict1 k) v))) true p2)
       (reduce (fn f [acc [k v]] (and acc (= (p/get-value dict2 k) v))) true p1))))

  (merge-dict [dict1 dict2]
    (->> (p/get-pairs dict2)
         (p/insert-all dict1)))

  (bind-dict [this f]
    (p/reduce-left this
                   (fn [acc-dict [k v]]
                     (p/merge-dict acc-dict (f k v)))
                   (OADict. (vec (repeat u/TABLE_SIZE nil)))))

  ;; Реализация стандартных интерфейсов Clojure
  clojure.lang.ILookup
  (valAt [dict k]
    (p/get-value dict k))
  (valAt [dict k not-found]
    (let [v (p/get-value dict k)]
      (if (nil? v) not-found v)))

  clojure.lang.Associative
  (assoc [dict k v]
    (p/insert dict k v))
  (containsKey [dict k]
    (p/contains-key? dict k))
  (entryAt [dict k]
    (when-let [v (p/get-value dict k)]
      (clojure.lang.MapEntry. k v)))

  clojure.lang.IPersistentMap
  (without [dict k]
    (p/delete dict k))

  clojure.lang.Seqable
  (seq [dict]
    (seq (map (fn [[k v]] (clojure.lang.MapEntry. k v))
              (p/get-pairs dict))))

  clojure.lang.Counted
  (count [_]
    (count (filter vector? table)))

  clojure.lang.IPersistentCollection
  (cons [this o]
    (cond
      (instance? clojure.lang.IMapEntry o)
      (p/insert this (key o) (val o))

      (and (vector? o) (= 2 (count o)))
      (p/insert this (o 0) (o 1))

      (map? o)
      (p/insert-all this o)

      (sequential? o)
      (p/insert-all this o)

      :else
      (throw (ex-info "Unsupported element for conj/cons on OADict" {:value o}))))

  (empty [_]
    (OADict. (vec (repeat u/TABLE_SIZE nil))))

  (equiv [this other]
    (cond
      (instance? OADict other)
      (p/equals-dict? this other)

      (map? other)
      (= (into {} (seq this)) other)

      :else false)))

(defn create-empty-dict
  "Создает пустой словарь с открытой адресацией"
  []
  (OADict. (vec (repeat u/TABLE_SIZE nil))))
