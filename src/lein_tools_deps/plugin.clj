(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as mvn]
            [clojure.pprint :as pp]
            (clojure.tools.deps.alpha.extensions
             [deps :as deps+]
             [git :as git]
             [local :as local]
             [maven :as maven])
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log])
  ;  [leiningen.core.project :as p]
  ;  [leiningen.core.main :as lein]))
 (:import [org.eclipse.aether RepositorySystem RepositorySystemSession]
          [org.eclipse.aether.spi.locator ServiceLocator]))

(def locator @mvn/the-locator)
(log/debug "locator" locator (.getService locator RepositorySystem))

(def system (mvn/make-system))
(log/debug "system" system)

(def system-deps-file (io/file "/usr/local/lib/clojure/deps.edn"))

(def project-deps-file (io/file "deps.edn"))

(defn home-deps-file []
  (io/file (System/getProperty "user.home") ".clojure" (io/file "deps.edn")))

(def location->dep-paths
  "Map deps.edn location names to paths"
  {:system system-deps-file
   :home (home-deps-file)
   :project project-deps-file})

(def default-deps [:system :home :project])

(defn canonicalise-dep-refs [dep-refs]
  (->> dep-refs
       (map #(location->dep-paths % %))
       (map io/file)))

(defn leinize [[proj coord]]
  [proj (:mvn/version coord)])

(defn resolve-deps [deps]
  (log/debug "deps.edn files" deps)
  (let [all-deps (filter #(.exists %) deps)
        _ (log/debug "exists" all-deps)
        deps-map (reader/read-deps all-deps)
        _ (pp/pprint deps-map)
        tdeps-map (deps/resolve-deps deps-map {:verbose true})
        _ (log/debug "resolved" tdeps-map)
        lein-deps-vector (mapv leinize tdeps-map)
        _ (log/debug "leinized" lein-deps-vector)
        project-deps {:dependencies lein-deps-vector}]

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
