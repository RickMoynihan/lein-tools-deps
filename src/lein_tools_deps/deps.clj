(ns lein-tools-deps.deps
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.tools.deps.alpha.reader :as reader]
            [lein-tools-deps.file-attributes :as file-attributes]))

(def default-clojure-executables ["/usr/local/bin/clojure"])

(defn- clojure-exe
  [config]
  (let [clojure-paths (or (:clojure-executables config) default-clojure-executables)
        exe (->> clojure-paths
                 (filter #(.exists (io/file %)))
                 first)]
    (or exe (throw (ex-info "Could not find clojure executable" {:tried-paths clojure-paths})))))

(defn- scrape-clojure-env
  [config]
  (let [exe (clojure-exe config)
        {:keys [out exit] :as result} (shell/sh exe "-Sdescribe")]
    (if (zero? exit)
      (read-string out)
      (throw (ex-info "Unable to locate Clojure's edn files" result)))))

(def clojure-env
  "Returns a map describing the environment known to clj/clojure:
  {:config-files [ ... ]}"
  (memoize scrape-clojure-env))

(defn make-dep-loc-lookup
  "Returns a function mapping from a loc(ation)
  keyword (either :install, :user or :project) to a file
  location.  If the value is a string it is returned as is."
  [config]
  (let [[system-deps home-deps project-deps] (:config-files (clojure-env config))
        project-deps (or project-deps "deps.edn")]
    (fn [i]
      (if (string? i)
        i
        ({:install system-deps
          :user home-deps
          :project project-deps} i)))))

(defn canonicalise-dep-locs
  "Returns a seq of absolute java.io.File given a seq of dep-refs.  Any
  relative dep-refs will be made absolute relative to project-root."
  [config project-root dep-refs]
  (let [location->dep-path (shell/with-sh-dir project-root (make-dep-loc-lookup config))]
    (->> dep-refs
         (map #(location->dep-path %))
         (map io/file)
         (map (partial file-attributes/absolute-file project-root)))))

(defn absolute-local-root-coords
  "Given a base path and :local/root coordinates, ensures the specified path
  is absolute relative to the base path."
  [{:keys [local/root]} base-path]
  {:local/root (file-attributes/absolute-path base-path root)})

(defn absolute-coords
  "Given a base path and dep coordinates, ensures any paths in the coordinates
  are absolute relative to the base path."
  [coords base-path]
  (if (contains? coords :local/root)
    (absolute-local-root-coords coords base-path)
    coords))

(defn absolute-deps-map
  "Given a base path and a deps map (a mapping from lib symbol to
  coordinates), ensures that any relative paths embedded in coordinates are
  absolute relative to the base path."
  [deps-map base-path]
  (->> deps-map
       (map (fn [[dep coords]]
              [dep (if (string? coords)
                     (file-attributes/absolute-path base-path coords)
                     (absolute-coords coords base-path))]))
       (into {})))

(defn- alias-absolute-deps-map
  "Given a base path and a map of aliases, ensure that any relative paths
  embedded in the extra-dep or classpath-overrides keys are absolute
  relative to the base path."
  [aliases base-path]
  (->> aliases
       (map (fn [[alias info]]
              [alias (->> [:extra-deps :classpath-overrides]
                          (select-keys info)
                          keys
                          (reduce (fn [acc k] (update acc k absolute-deps-map base-path)) info))]))
       (into {})))

(defn absolute-deps
  "Given a base path and deps, ensures that all absolute paths in the deps
  (including relative deps embedded in aliases) are absolute relative to the
  base path."
  [deps base-path]
  (-> deps
      (update :deps absolute-deps-map base-path)
      (update :aliases alias-absolute-deps-map base-path)))

(defn make-deps
  "Reads and merges all of the deps-ref, returning a single deps map"
  [{:keys [root] {:keys [config-files] :as config} :lein-tools-deps/config :as project}]
  (as-> config-files $
        (canonicalise-dep-locs config (:root project) $)
        (filter #(.exists %) $)
        (reader/read-deps $)
        (absolute-deps $ root)))

