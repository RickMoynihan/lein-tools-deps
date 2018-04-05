(defproject lein-tools-deps "0.1.0-SNAPSHOT"
  :description "Leiningen plugin to load :dependencies from deps.edn instead."
  :url "https://github.com/RickMoynihan/lein-tools-deps"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.deps.alpha "0.5.417"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.apache.maven/maven-resolver-provider "3.5.2"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :exclusions [org.slf4j/slf4j-nop]

  ;:plugins [[thomasa/mranderson "0.4.8"]]
  :eval-in-leiningen true)
