(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [leiningen.core.project :as p]
            [leiningen.core.main :as lein])
  (:import (java.io File)))

;; load extensions
(require 'clojure.tools.deps.alpha.extensions.deps)
(require 'clojure.tools.deps.alpha.extensions.git)
(require 'clojure.tools.deps.alpha.extensions.local)
(require 'clojure.tools.deps.alpha.extensions.maven)

(defn make-dep-loc-lookup
  "Returns a function mapping from a loc(ation)
  keyword (either :system, :home or :project) to a file
  location.  If the value is a string it is returned as is."
  []
  (let [[system-deps home-deps project-deps] (:config-files (reader/clojure-env))
        project-deps (or project-deps "deps.edn")]
    (fn [i]
      (if (string? i)
        i
        ({:system system-deps
          :home home-deps
          :project project-deps} i)))))

(defn ^File absolute-file
  "Takes an absolute base path and a potentially relative file and returns an
  absolute file, using the base to to form the absolute file if needed."
  [base-path path]
  (let [file (io/file path)]
    (if (not (.isAbsolute file))
      (io/file base-path file)
      file)))

(defn absolute-path
  "Takes an absolute base path and a potentially relative file and returns an
  absolute path (string), using the base to to form the absolute file if
  needed."
  [base-path path]
  (.getAbsolutePath (absolute-file base-path path)))

(defn canonicalise-dep-locs
  "Returns a seq of absolute java.io.File given a seq of dep-refs.  Any
  relative dep-refs will be made absolute relative to project-root."
  [project-root dep-refs]
  (let [location->dep-path (shell/with-sh-dir project-root (make-dep-loc-lookup))]
    (->> dep-refs
         (map #(location->dep-path %))
         (map io/file)
         (map (partial absolute-file project-root)))))

(defn read-all-deps [deps-files]
  (-> deps-files
      reader/read-deps))

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

(defn lein-source-paths [project-root merged-deps tdeps]
  {:source-paths (->> tdeps
                      (filter-by-manifest :deps)
                      (mapv leinize)
                      (apply concat)
                      (into (:paths merged-deps))
                      (map (partial absolute-path project-root)))})

(defn absolute-local-root-coords
  "Given a base path and :local/root coordinates, ensures the specified path
  is absolute relative to the base path."
  [base-path {:keys [local/root]}]
  {:local/root (absolute-path base-path root)})

(defn absolute-coords
  "Given a base path and dep coordinates, ensures any paths in the coordinates
  are absolute relative to the base path."
  [base-path coords]
  (if (contains? coords :local/root)
    (absolute-local-root-coords base-path coords)
    coords))

(defn absolute-deps-map
  "Given a base path and a deps map (a mapping from lib symbol to
  coordinates), ensures that any relative paths embedded in coordinates are
  absolute relative to the base path."
  [base-path deps-map]
  (->> deps-map
       (map (fn [[dep coords]]
              [dep (absolute-coords base-path coords)]))
       (into {})))

(defn absolute-deps
  "Given a base path and deps, ensures that all absolute paths in the deps
  (including relative deps embedded in aliases) are absolute relative to the
  base path."
  [base-path deps]
  (-> deps
    (update :deps (partial absolute-deps-map base-path))
    (update :aliases (fn [aliases]
                       (->> aliases
                            (map (fn [[alias info]]
                                   [alias (if (contains? info :extra-deps)
                                            (update info :extra-deps (partial absolute-deps-map base-path))
                                            info)]))
                            (into {}))))))

(defn resolve-deps
  "Takes a seq of java.io.File objects pointing to deps.edn files
  and merges them all before resolving their dependencies.

  Returns a {:dependencies [coordinates]} datastructure suitable for
  meta-merging into a lein project map."
  [project-root deps]
  (let [all-deps (filter #(.exists %) deps)
        deps (read-all-deps all-deps)
        deps (absolute-deps project-root deps)
        tdeps-map (deps/resolve-deps deps {})]
    (merge (lein-dependencies tdeps-map)
           (lein-source-paths project-root deps tdeps-map))))

(defn loc-or-string? [l]
  (or (#{:system :home :project} l) (string? l)))

(defn middleware
  "Inject relevant keys from deps.edn files into the leiningen project map
  while honoring other user-supplied config."
  [{{:keys [config-files] :as config} :tools/deps :as project}]
  (if (seq config-files)
    (if (every? loc-or-string? config-files)
      (->> config-files
           (canonicalise-dep-locs (:root project))
           (resolve-deps (:root project))
           (merge project))
      (do (lein/warn  "Every element in :tools/deps :config-files must either be a file-path string or one of the locations :system, :project, or :home.")
          (lein/exit 1)))
    project))

(comment
  (read-all-deps (canonicalise-dep-locs "/users/foo/proj" [:system :home "example/deps.edn"]))

  (resolve-deps (canonicalise-dep-locs "/users/foo/proj" [:system :home "example/deps.edn" "foo"])))
