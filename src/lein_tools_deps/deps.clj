(ns lein-tools-deps.deps
  (:require [clojure.java.io :as io]
            [lein-tools-deps.file-attributes :as file-attributes]
            [lein-tools-deps.env :as env]
            [clojure.tools.deps.alpha.reader :as reader]))

(defn make-dep-loc-lookup
  "Returns a function mapping from a loc(ation)
  keyword (either :install, :user or :project) to a file
  location.  If the value is a string it is returned as is."
  [{:keys [config-files]}]
  (let [[system-deps home-deps project-deps] config-files
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
  [env project-root dep-refs]
  (let [location->dep-path (make-dep-loc-lookup env)]
    (->> dep-refs
         (map location->dep-path)
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
  ([exists? read-deps env {:keys [root] {:keys [config-files]} :lein-tools-deps/config}]
   (as-> config-files $
         (canonicalise-dep-locs env root $)
         (filter exists? $)
         (read-deps $)
         (absolute-deps $ root)))
  ([env project]
    (make-deps env/exists? reader/read-deps env project)))

