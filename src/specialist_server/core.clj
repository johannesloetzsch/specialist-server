(ns specialist-server.core
  (:require [clojure.walk :as walk]
            [clojure.pprint :refer [pprint]]
            [specialist-server.parser :as p]
            [specialist-server.parserb :as pb]
            [specialist-server.resolver :as r]
            [specialist-server.introspection :as i]))


;;;DEBUG helper fn
(defn execute-b [query op-name context info]
  (if (nil? op-name)
    (-> query pb/parse vals first (r/run context info))
    (if-let [op (-> query pb/parse (get op-name))]
      (r/run op context info)
      (throw (ex-info (str "Query error: no such operation") {:name op-name})))))

;;;

(defn- deferred
  "Run deferred resolver functions. Uses breadth-first traversal."
  [data]
  (letfn [(tuples [key-root data]
            (if (map? data)
              (map (fn [[k v]] [(conj key-root k) v]) data)
              [[key-root data]]))]

    (loop [data-queue [{} (tuples [] data)]]
      (let [[out queue] data-queue]
        (if (empty? queue)
          out
          (recur (reduce (fn [[coll q] [k node]]
                           (let [v (if (fn? node) (node) node)]
                             (cond
                               (not (coll? v))
                               [(assoc-in coll k v)
                                q]

                               (map? v)
                               [(assoc-in coll k {})
                                (into q (tuples k v))]

                               :else
                               [(assoc-in coll k [])
                                (reduce into q (map #(tuples (conj k %1) %2) (range 0 (count v)) v))])))
                         [out []]
                         queue)))))))

(defn- execute [query schema context info]
  (try
    (let [fun (p/parse query)]
      (if (:deferred? info)
        (deferred (fun schema context info))
        (fun schema context info)))
    (catch clojure.lang.ExceptionInfo ex
      (.write *out*
              (with-out-str
                (println (:id info) ">>>")
                (println (.getMessage ex))
                (pprint (ex-data ex))
                (println "<<<" (:id info))
                ))
      (when *flush-on-newline* (flush))
      {:errors [{:message (.getMessage ex)
                 :query_id (:id info)}]})))

(defn server [schema]
  (let [type-map (i/type-map schema)
        ; Internal schema map: add introspection fields.
        inner-schema (-> schema
                         (assoc-in [:query :__schema] #'i/__schema)
                         (assoc-in [:query :__type]   #'i/__type))]
    (fn [{:keys [query variables root context deferred?]}]
      (let [info {:id (str (java.util.UUID/randomUUID))
                  :schema schema
                  :type-map type-map
                  :variable-values (or variables {}) ;(reduce-kv (fn [m k v] (assoc m (keyword k) v )) {} variables)
                  :root-value (or root {})
                  :deferred? (boolean deferred?)}]
        (execute query inner-schema (or context {}) info)))))

(defn executor
  "Alias for specialist-server.core/server"
  [schema]
  (server schema))
