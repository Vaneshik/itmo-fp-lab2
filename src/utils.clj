(ns utils)

(def TABLE_SIZE 32)

(defn my-reduce-right
  "Правая свёртка коллекции"
  [f init coll]
  (if (empty? coll)
    init
    (f (my-reduce-right f init (rest coll)) (first coll))))

(defn locate-empty-cell
  "Найти свободную ячейку для вставки с linear probing"
  [table key start-idx]
  (loop [idx start-idx
         attempts 0]
    (if (>= attempts (count table))
      nil
      (let [slot (nth table idx)]
        (cond
          (or (nil? slot) (= slot :deleted)) idx
          (and (vector? slot) (= (first slot) key)) idx
          :else (recur (mod (inc idx) (count table)) (inc attempts)))))))

(defn search-key
  "Найти ячейку с заданным ключом"
  [table key start-idx]
  (loop [idx start-idx
         attempts 0]
    (if (>= attempts (count table))
      nil
      (let [slot (nth table idx)]
        (cond
          (nil? slot) nil
          (and (vector? slot) (= (first slot) key)) idx
          :else (recur (mod (inc idx) (count table)) (inc attempts)))))))

