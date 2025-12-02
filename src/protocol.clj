(ns protocol)

(defprotocol Dict
  "словарь на основе хеш-таблицы с открытой адресацией"
  (insert [dict key value])
  (insert-all [dict pairs])
  (get-value [dict key])
  (contains-key? [dict key])
  (get-keys [dict])
  (get-values [dict])
  (get-pairs [dict])
  (delete [dict key])
  (filter-dict [dict pred])
  (map-dict [dict f])
  (reduce-left [dict f init])
  (reduce-right [dict f init])
  (equals-dict? [dict1 dict2])
  (merge-dict [dict1 dict2])
  (bind-dict [dict f]))

