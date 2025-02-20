(defproject ajk/specialist-server "0.7.0"
  :description "Spec-driven Clojure GraphQL server"
  :url "https://github.com/ajk/specialist-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.logging "1.2.3"]
                 [clj-antlr "0.2.10"]]
  :profiles {:test {:dependencies [[org.clojure/test.check "0.9.0"]]}}

  :min-lein-version "2.9.1"
  :pedantic :abort)
