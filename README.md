# Лабораторная работа №2

---

Выполнил: Чусовлянов Максим Сергеевич

Группа: P3307

Преподаватель: Пенской Александр Владимирович

Вариант: `oa-dict`

---

## Требования

1. Функции:

    - добавление и удаление элементов;

    - фильтрация;

    - отображение (map);

    - свертки (левая и правая);

    - структура должна быть моноидом.

2. Структуры данных должны быть неизменяемыми.

3. Библиотека должна быть протестирована в рамках unit testing.

4. Библиотека должна быть протестирована в рамках property-based тестирования (как минимум 3 свойства, включая свойства моноида).

5. Структура должна быть полиморфной.

6. Требуется использовать идиоматичный для технологии стиль программирования. Примечание: некоторые языки позволяют получить большую часть API через реализацию небольшого интерфейса. Так как лабораторная работа про ФП, а не про экосистему языка -- необходимо реализовать их вручную и по возможности -- обеспечить совместимость.

7. Обратите внимание:

    - API должно быть реализовано для заданного интерфейса и оно не должно "протекать". На уровне тестов -- в первую очередь нужно протестировать именно API (dict, set, bag).

    - Должна быть эффективная реализация функции сравнения (не наивное приведение к спискам, их сортировка с последующим сравнением), реализованная на уровне API, а не внутреннего представления.

## Реализация

### Что это такое

OpenAddress Hashmap — хеш-таблица, где все пары ключ-значение хранятся прямо в одном массиве на 32 ячейки. Каждая ячейка может быть в трёх состояниях:
- `nil` — свободна
- `:deleted` — была занята, но элемент удалили
- `[key value]` — содержит пару

### Как разрешаются коллизии

Используется linear probing: если ячейка занята, пробуем следующую, потом ещё следующую... `(hash(key) + i) mod TABLE_SIZE`, пока не найдём свободное место.

### Зачем маркер удаления

Нельзя просто ставить `nil` при удалении — сломается поиск других элементов! Маркер `:deleted` говорит "ячейка свободна для вставки, но продолжай искать дальше".

### Интерфейс

Словарь определён через протокол `Dict`:

```clj
(defprotocol Dict
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
```

Реализация через `deftype OADict` с одним полем `table` — вектор ячеек:

```clj
(deftype OADict [table]
  Dict
  (insert [dict key value] ...)
  (get-value [dict key] ...)
  ...)
```

### Почему и defprotocol, и стандартные интерфейсы?

Используется двойной подход:

1. **Свой протокол `Dict`** — основное API
2. **Стандартные интерфейсы Clojure** — для совместимости

Зачем свой протокол? Требования говорят: "реализовать вручную, API не должно протекать". Протокол `Dict` даёт:

- Явные функции `insert`, `delete`, `filter-dict` и т.д. — всё руками
- Тесты не зависят от деталей реализации
- Можно сделать другую реализацию (на дереве, например) с тем же интерфейсом

Зачем стандартные интерфейсы? Требования: "обеспечить совместимость". `ILookup`, `Associative`, `Seqable` и остальные позволяют:

- Работать через обычные `get`, `assoc`, `seq`, `count`
- Использовать стандартные `map`, `reduce`, `filter`
- Интегрироваться с любым Clojure-кодом

Примеры:

**Через протокол:**
```clojure
(let [d (-> (dict/empty-dict)
            (dict/insert :a 1)
            (dict/insert :b 2))]
  (dict/get-value d :a))  ; => 1
```

**Через стандартные интерфейсы:**
```clojure
(let [d (-> (dict/empty-dict)
            (assoc :a 1)
            (assoc :b 2))]
  (get d :a))  ; => 1
```

### Основные алгоритмы

#### Поиск свободной ячейки

`locate-empty-cell` ищет куда вставить, используя linear probing:

```clj
(defn- locate-empty-cell [table key start-idx]
  (loop [idx start-idx, attempts 0]
    (if (>= attempts (count table))
      nil
      (let [slot (nth table idx)]
        (cond
          (or (nil? slot) (= slot :deleted)) idx
          (and (vector? slot) (= (first slot) key)) idx
          :else (recur (mod (inc idx) (count table)) (inc attempts)))))))
```

#### Поиск по ключу

`search-key` находит ячейку с нужным ключом:

```clj
(defn- search-key [table key start-idx]
  (loop [idx start-idx, attempts 0]
    (if (>= attempts (count table))
      nil
      (let [slot (nth table idx)]
        (cond
          (nil? slot) nil
          (and (vector? slot) (= (first slot) key)) idx
          :else (recur (mod (inc idx) (count table)) (inc attempts)))))))
```

#### Вставка

Находим ячейку и создаём новый словарь:

```clj
(insert [dict key value]
  (let [idx (mod (hash key) (count table))
        slot-idx (locate-empty-cell table key idx)]
    (if slot-idx
      (OADict. (assoc table slot-idx [key value]))
      (throw (ex-info "HashMap is full" {:key key :value value})))))
```

#### Получение значения

Используем `search-key` чтобы найти ячейку:

```clj
(get-value [dict key]
  (let [idx (mod (hash key) (count table))
        slot-idx (search-key table key idx)]
    (when slot-idx
      (let [slot (nth table slot-idx)]
        (when (vector? slot) (second slot))))))
```

#### Удаление

Помечаем ячейку маркером `:deleted`:

```clj
(delete [dict key]
  (let [idx (mod (hash key) (count table))
        slot-idx (search-key table key idx)]
    (if slot-idx
      (OADict. (assoc table slot-idx :deleted))
      dict)))
```

#### Фильтрация

Функция `pred` принимает пару `[key value]`:

```clj
(filter-dict [dict pred]
  (->> (get-pairs dict)
       (filter pred)
       (insert-all (OADict. (vec (repeat N_BUCKETS nil))))))
```

#### Отображение (map)

Функция `f` принимает `[key value]` и возвращает `[key value]`:

```clj
(map-dict [dict f]
  (->> (get-pairs dict)
       (map f)
       (insert-all (OADict. (vec (repeat N_BUCKETS nil))))))
```

#### Свёртки (reduce)

Функция `f` принимает `[acc [key value]]`, где `acc` — аккумулятор:

```clj
(reduce-left [dict f init]
  (->> (get-pairs dict)
       (reduce f init)))

(reduce-right [dict f init]
  (->> (get-pairs dict)
       (my-reduce-right f init)))
```

#### Монада: bind

`bind-dict` это монадический flatMap. Функция `f` применяется к каждой паре `[key value]` и возвращает словарь. Все результаты мержатся:

```clj
(bind-dict [dict f]
  (reduce-left dict
               (fn [acc-dict [k v]]
                 (merge-dict acc-dict (f k v)))
               (empty-dict)))
```

Примеры:
```clojure
(let [d (-> (dict/empty-dict)
            (dict/insert :a 1)
            (dict/insert :b 2))
      result (dict/bind-dict d 
               (fn [k v] 
                 (-> (dict/empty-dict)
                     (dict/insert k v)
                     (dict/insert (keyword (str (name k) "-doubled")) (* v 2)))))]
  (dict/get-pairs result))
; => [[:a 1] [:a-doubled 2] [:b 2] [:b-doubled 4]]
```

Агрегация (последнее значение остаётся из-за merge):
```clojure
(let [d (dict/dict-from [[:x 5] [:y 10]])
      result (dict/bind-dict d
               (fn [k v]
                 (dict/insert (dict/empty-dict) :sum v)))]
  (dict/get-value result :sum))
; => 10
```

Монадические законы выполняются:
- Левая идентичность: `bind(return(x), f) = f(x)`
- Правая идентичность: `bind(m, return) = m`
- Ассоциативность: `bind(bind(m, f), g) = bind(m, λx.bind(f(x), g))`

### Вспомогательные функции

Конструкторы:

```clj
(defn empty-dict []
  (OADict. (vec (repeat TABLE_SIZE nil))))

(defn dict-from [pairs]
  (insert-all (empty-dict) pairs))
```

Правая свёртка (стандартный `reduce` только слева):

```clj
(defn my-reduce-right [f init coll]
  (if (empty? coll)
    init
    (f (my-reduce-right f init (rest coll)) (first coll))))
```

### Свойства

**Неизменяемость:** Все операции возвращают новый словарь.

**Моноид:** Операция `merge-dict` + нейтральный элемент `empty-dict`:
- `(merge-dict (empty-dict) d) ≡ d`
- `(merge-dict d (empty-dict)) ≡ d`
- `(merge-dict (merge-dict d1 d2) d3) ≡ (merge-dict d1 (merge-dict d2 d3))`

**Полиморфизм:** Протокол `Dict` позволяет делать разные реализации с одним интерфейсом.

Код: [core.clj](src/oa_dict/core.clj)

## Тестирование

Unit-тесты (`clojure.test`) + property-based тесты (`test.check`).

Код: [unit_tests](test/oa_dict/unit_tests.clj) и [property_tests](test/oa_dict/property_tests.clj)

### Композитные генераторы с gen/let

Для более качественных тестов используем `gen/let` для создания зависимых генераторов:

```clojure
;; Генератор словаря
(def gen-dict
  (gen/let [pairs gen-pairs]
    (dict/dict-from pairs)))

;; Генератор непустого словаря (гарантированно ≥1 элемент)
(def gen-non-empty-dict
  (gen/let [first-pair (gen/tuple gen-key gen-value)
            rest-pairs gen-pairs]
    (dict/dict-from (cons first-pair rest-pairs))))

;; Генератор словаря с известным ключом для тестирования поиска
(def gen-dict-with-known-key
  (gen/let [known-key gen-key
            known-val gen-value
            other-pairs gen-pairs]
    {:dict (dict/insert (dict/dict-from other-pairs) known-key known-val)
     :key known-key
     :value known-val}))
```

Преимущества:
- Зависимые генераторы (одно зависит от другого)
- Более реалистичные данные
- Явные связи между значениями

Пример теста:

```clojure
(defspec property-contains-inserted-elements ITERATIONS
  (gen/let [{:keys [dict key]} gen-dict-with-known-key]
    (dict/contains-key? dict key)))
```

Тест гарантирует, что проверяемый ключ действительно есть в словаре.

### Стандартные интерфейсы

Реализованы:

- `ILookup` — `valAt` (получить по ключу)
- `Associative` — `assoc`, `containsKey`, `entryAt`
- `IPersistentMap` — `without` (удалить)
- `Seqable` — `seq` (последовательность)
- `Counted` — `count`
- `IPersistentCollection` — `cons`, `empty`, `equiv`

Все протестированы.

### Кастомный вывод ошибок

При несовпадении словарей показываем:
- `only-in-1` — есть только в первом
- `only-in-2` — есть только во втором  
- `mismatched` — ключ есть в обоих, но значения разные

Реализация: [assertions.clj](test/oa_dict/assertions.clj) и [pretty_reporter.clj](test/oa_dict/pretty_reporter.clj)

## Выводы

Реализована хеш-таблица с открытой адресацией и linear probing. По сравнению с separate chaining более компактна — все элементы в одном массиве.

Что сделано:
- Полностью функциональная неизменяемая структура
- Стандартные интерфейсы Clojure (работает со всей экосистемой)
- Моноид + монада (bind-dict с монадическими законами)
- Unit-тесты + property-based тесты с `gen/let`

Особенности:
- Маркеры `:deleted` для корректного поиска после удаления
- Эффективное сравнение без сортировки
- Двойное API: свой протокол + стандартные интерфейсы
- Композитные генераторы для качественного тестирования

Работа дала хорошее понимание ФП на Clojure, персистентных структур и монад.

