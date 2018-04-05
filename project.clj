(defproject lein-tools-deps "0.1.0-SNAPSHOT"
  :description "Leiningen plugin to load :dependencies from deps.edn instead."
  :url "https://github.com/RickMoynihan/lein-tools-deps"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.deps.alpha "0.5.418-SNAPSHOT"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ch.qos.logback/logback-classic "1.0.13"]]
  :exclusions [org.slf4j/slf4j-nop]

  ;:plugins [[thomasa/mranderson "0.4.8"]]
  :eval-in-leiningen true)
