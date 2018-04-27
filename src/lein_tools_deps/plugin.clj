(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [leiningen.core.project :as p]
            [leiningen.core.main :as lein]))

;; load extensions
(require 'clojure.tools.deps.alpha.extensions.deps)
(require 'clojure.tools.deps.alpha.extensions.git)
(require 'clojure.tools.deps.alpha.extensions.local)
(require 'clojure.tools.deps.alpha.extensions.maven)

(def config-files (:config-files (reader/clojure-env)))

(def system-deps (io/file (config-files 0)))

(def home-deps (io/file (config-files 1)))

(def deps-file (io/file (config-files 2)))

(def location->dep-paths
  "Map deps.edn location names to paths"
  {:system system-deps
   :home home-deps
   :project deps-file})

(def default-deps [:system :home :project])

(defn canonicalise-dep-refs [dep-refs]
  (->> dep-refs
       (map #(location->dep-paths % %))
       (map io/file)))

(defn read-all-deps [deps-files]
  (-> deps-files
      reader/read-deps
      (deps/resolve-deps {})))

(defmulti leinize (fn [[dep-key dep-val]]
                    (:deps/manifest dep-val)))

(defmethod leinize :mvn [[artifact info]]
  ;; Thanks to @seancorfield and boot-tools-deps for this snippet
  (transduce cat conj [artifact (:mvn/version info)]
             (select-keys info
                          [:classifier
                           :extension
                           :exclusions
                           :scope])))

(defmethod leinize :deps [[artifact info]]
  (:paths info))

(defn filter-by-manifest [manifest-type tdeps]
  (filter (fn [[artifact info]]
            (= manifest-type (:deps/manifest info)))
          tdeps))

(defn lein-dependencies [tdeps]
  {:dependencies (->> tdeps
                      (filter-by-manifest :mvn)
                      (mapv leinize))})

(defn lein-source-paths [tdeps]
  {:source-paths (->> tdeps
                      (filter-by-manifest :deps)
                      (mapv leinize)
                      (apply concat)
                      vec)})

(defn resolve-deps
  "Takes a seq of java.io.File objects pointing to deps.edn files
  and merges them all before resolving their dependencies.

  Returns a {:dependencies [coordinates]} datastructure suitable for
  meta-merging into a lein project map."
  [deps]
  (let [all-deps (filter #(.exists %) deps)

        tdeps-map (read-all-deps all-deps)]

    (merge (lein-dependencies tdeps-map)
           (lein-source-paths tdeps-map))))


(defn middleware
  "Inject relevant keys from deps.edn files into the leiningen project map."
  [{deps-files :tools/deps :as project}]
  (if (seq deps-files)
    (->> deps-files
         canonicalise-dep-refs
         resolve-deps
         (merge project))
    project))

(comment
  (read-all-deps (canonicalise-dep-refs [:system :home "example/deps.edn"]))

  {org.clojure/clojure {:mvn/version "1.9.0", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar"]}
   , criterium/criterium {:mvn/version "0.4.4", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/criterium/criterium/0.4.4/criterium-0.4.4.jar"]},
   org.clojure/tools.nrepl {:mvn/version "0.2.12", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/org/clojure/tools.nrepl/0.2.12/tools.nrepl-0.2.12.jar"]}
   , github-puredanger/demo-deps {:git/url "https://github.com/puredanger/demo-deps", :sha "19d387dc11d804ab955207a263dfba5dbd15bf2c", :deps/manifest :deps, :deps/root "/Users/rick/.gitlibs/libs/github-puredanger/demo-deps/19d387dc11d804ab955207a263dfba5dbd15bf2c", :paths ["/Users/rick/.gitlibs/libs/github-puredanger/demo-deps/19d387dc11d804ab955207a263dfba5dbd15bf2c/src"]}
   , org.clojure/spec.alpha {:mvn/version "0.1.143", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/org/clojure/spec.alpha/0.1.143/spec.alpha-0.1.143.jar"], :dependents [org.clojure/clojure]}
   , org.clojure/core.specs.alpha {:mvn/version "0.1.24", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/org/clojure/core.specs.alpha/0.1.24/core.specs.alpha-0.1.24.jar"], :dependents [org.clojure/clojure]}
   , clj-time/clj-time {:mvn/version "0.14.2", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/clj-time/clj-time/0.14.2/clj-time-0.14.2.jar"], :dependents [github-puredanger/demo-deps]}
   , joda-time/joda-time {:mvn/version "2.9.7", :deps/manifest :mvn, :paths ["/Users/rick/.m2/repository/joda-time/joda-time/2.9.7/joda-time-2.9.7.jar"], :dependents [clj-time/clj-time]}}
  
  (resolve-deps (canonicalise-dep-refs [:system :home "example/deps.edn"]))

  
  
  )
