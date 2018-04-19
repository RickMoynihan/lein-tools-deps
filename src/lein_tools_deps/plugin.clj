(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
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

(defn leinize [[proj coord]]
  [proj (:mvn/version coord)])

(defn resolve-deps [deps]
  (let [all-deps (->> deps
                      (filter #(.exists %)))

        tdeps-map (-> all-deps
                      reader/read-deps
                      (deps/resolve-deps {}))

        lein-deps-vector (->> tdeps-map
                              (mapv leinize))
        
        project-deps {:dependencies lein-deps-vector }]

    project-deps))


(defn middleware
  "Inject dependencies from deps.edn files into the
  leiningen :dependencies vector."
  [{deps-files :tools/deps :as project}]
  (if (seq deps-files)
    (->> deps-files
         canonicalise-dep-refs
         resolve-deps
         (merge project))
    project))

(comment
  (resolve-deps (canonicalise-dep-refs [:system :home "example/deps.edn"]))

  
  
  )
