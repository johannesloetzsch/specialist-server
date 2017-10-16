(ns specialist-server.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [specialist-server.type :as t]
            [specialist-server.core :refer [executor]]))

(defn happy
  "See if our greeting is happy or not."
  [node opt ctx info]
  (boolean (re-find #"!\s*$" (:greeting node))))

(defn hello
  "Basic example resolver."
  [node opt ctx info]
  (let [n (get opt :name "world!")]
    {:greeting (str "Hello " n)
     :happy    #'happy}))

;;;

(s/def ::name     (s/nilable (t/field t/string "Recipient of our greeting.")))
(s/def ::greeting t/string)

(s/def ::happy (t/resolver #'happy))

(s/def ::hello-node (s/keys :req-un [::greeting ::happy]))


(s/fdef happy
        :args (s/tuple ::hello-node map? map? map?)
        :ret t/boolean)

(s/fdef hello
        :args (s/tuple map? (s/keys :opt-un [::name]) map? map?)
        :ret ::hello-node)


(def graphql (executor {:query {:hello #'hello}}))

;;;

(deftest hello-world
  (testing "Basic queries"
    (let [res-happy (-> {:query "{hello {greeting happy}}"}               graphql :data :hello)
          res-meh   (-> {:query "{hello(name:\"meh\") {greeting happy}}"} graphql :data :hello)]
      (is (= "Hello world!" (:greeting res-happy)))
      (is (= true (:happy res-happy)))
      (is (= "Hello meh" (:greeting res-meh)))
      (is (= false (:happy res-meh)))))

  (testing "Queriy with a variable"
    (let [q {:query "query Hello($name:String) { hello(name:$name) { greeting }}"
             :variables {:name "Clojure!"}}
          res (-> q graphql :data :hello)]
      (is (= "Hello Clojure!" (:greeting res)))))

  (testing "Introspection"
    (is (= {:data
            {:__type
             {:fields '({:type {:ofType {:name "String"}}, :name "greeting"}
                        {:type {:ofType {:name "Boolean"}}, :name "happy"})}}}
           (graphql {:query "{__type(name:\"hello\") { fields { type { ofType { name} } name }}}"})))

    (is (= "hello" (-> {:query "{hello {__typename}}"} graphql :data :hello :__typename)))

    ))

