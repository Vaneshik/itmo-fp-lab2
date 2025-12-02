(ns core
  (:require [protocol :as api]
            [impl :as impl]))

(def empty-dict
  "Пустой словарь с открытой адресацией"
  (impl/create-empty-dict))

(defn insert
  "Добавляет пару ключ-значение в словарь"
  [dict key value]
  (api/insert dict key value))

(defn insert-all
  "Добавляет несколько пар в словарь"
  [dict pairs]
  (api/insert-all dict pairs))

(defn get-value
  "Получает значение по ключу"
  [dict key]
  (api/get-value dict key))

(defn contains-key?
  "Проверяет наличие ключа в словаре"
  [dict key]
  (api/contains-key? dict key))

(defn get-keys
  "Возвращает все ключи словаря"
  [dict]
  (api/get-keys dict))

(defn get-values
  "Возвращает все значения словаря"
  [dict]
  (api/get-values dict))

(defn get-pairs
  "Возвращает все пары ключ-значение"
  [dict]
  (api/get-pairs dict))

(defn delete
  "Удаляет элемент по ключу"
  [dict key]
  (api/delete dict key))

(defn filter-dict
  "Фильтрует словарь по предикату"
  [dict pred]
  (api/filter-dict dict pred))

(defn map-dict
  "Преобразует элементы словаря"
  [dict f]
  (api/map-dict dict f))

(defn reduce-left
  "Левая свёртка словаря"
  [dict f init]
  (api/reduce-left dict f init))

(defn reduce-right
  "Правая свёртка словаря"
  [dict f init]
  (api/reduce-right dict f init))

(defn equals-dict?
  "Проверяет равенство двух словарей"
  [dict1 dict2]
  (api/equals-dict? dict1 dict2))

(defn merge-dict
  "Объединяет два словаря (операция моноида)"
  [dict1 dict2]
  (api/merge-dict dict1 dict2))

(defn bind-dict
  "Монадическая операция bind"
  [dict f]
  (api/bind-dict dict f))

(defn dict-from
  "Создает словарь из последовательности пар [key value]"
  [pairs]
  (api/insert-all empty-dict pairs))
