(ns leiningen.tools-deps
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(require 'clojure.tools.deps.alpha.providers.maven)
(require 'clojure.tools.deps.alpha.providers.git)
(require 'clojure.tools.deps.alpha.providers.local)

(def system-deps (io/file "/usr/local/Cellar/clojure/1.9.0.273/deps.edn"))

(defn home-deps []
  (io/file (System/getenv "HOME") ".clojure" "deps.edn"))

(defn resolve-deps []
  (let [pwd-deps (io/file "deps.edn")
        all-deps (filter #(.exists %) [system-deps (home-deps) pwd-deps])]

    (->> (-> all-deps
             reader/read-deps
             (deps/resolve-deps {}))
         (map (fn [[proj coord]]
               [proj (:mvn/version coord)])))))

(comment

  (resolve-deps)
  
  

  )

(defn tools-deps
  "I don't do a lot."
  [project & args]
  (println "Hi!" project))
