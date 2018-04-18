(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
	    [org.satta.glob :as glob]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [leiningen.core.project :as p]
            [leiningen.core.main :as lein]))

#_(require 'clojure.tools.deps.alpha.extensions.deps) ;; broken ns in v0.3.260 should be fixed soon...
(require 'clojure.tools.deps.alpha.extensions.git)
(require 'clojure.tools.deps.alpha.extensions.local)
(require 'clojure.tools.deps.alpha.extensions.maven)

; https://github.com/RickMoynihan/lein-tools-deps/issues/6
(defn system-deps-file []
  (first
   (filter #(.exists %)
     (into [(io/file "/usr/local/lib/clojure/deps.edn")]
        (glob/glob "/usr/local/Cellar/clojure/1.9.*/deps.edn")))))

(def deps-file (io/file "deps.edn"))

(defn home-deps []
  (io/file (System/getProperty "user.home") ".clojure" deps-file))

(def location->dep-paths
  "Map deps.edn location names to paths"
  {:system (system-deps-file)
   :home (home-deps)
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
