(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            (clojure.tools.deps.alpha.extensions
             [deps :as deps+]
             [git :as git]
             [local :as local]
             [maven :as maven])
            [clojure.java.io :as io]
            [clojure.edn :as edn]))
          ;  [leiningen.core.project :as p]
          ;  [leiningen.core.main :as lein]))

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
  (let [all-deps (->> deps
                      (filter #(.exists %)))

        tdeps-map (-> all-deps
                      reader/read-deps
                      (deps/resolve-deps {:verbose true}))

        lein-deps-vector (->> tdeps-map
                              (mapv leinize))

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

(comment
  (resolve-deps (canonicalise-dep-refs [:system :home "example/deps.edn"]))
  (canonicalise-dep-refs [:system :home "example/deps.edn"])