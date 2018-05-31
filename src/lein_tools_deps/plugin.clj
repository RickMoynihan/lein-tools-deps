(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [leiningen.core.main :as lein])
  (:import (java.io File)))

;; load extensions
(require 'clojure.tools.deps.alpha.extensions.deps)
(require 'clojure.tools.deps.alpha.extensions.git)
(require 'clojure.tools.deps.alpha.extensions.local)
(require 'clojure.tools.deps.alpha.extensions.maven)

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
  [config project-root dep-refs]
  (let [location->dep-path (shell/with-sh-dir project-root (make-dep-loc-lookup config))]
    (->> dep-refs
         (map #(location->dep-path %))
         (map io/file)
         (map (partial absolute-file project-root)))))

(defn read-all-deps [deps-files]
  (-> deps-files
      reader/read-deps))

(defmulti leinize (fn [[_dep-key dep-val]]
                    (:deps/manifest dep-val)))

(defmethod leinize :mvn [[artifact info]]
  ;; Thanks to @seancorfield and boot-tools-deps for this snippet
  (transduce cat conj [artifact (:mvn/version info)]
             (select-keys info
                          [:classifier
                           :extension
                           :exclusions
                           :scope])))

(defmethod leinize :deps [[_artifact info]]
  (:paths info))

(defn filter-by-manifest [manifest-type tdeps]
  (filter (fn [[_artifact info]]
            (= manifest-type (:deps/manifest info)))
          tdeps))

(defn lein-dependencies [tdeps]
  (->> tdeps
       (filter-by-manifest :mvn)
       (mapv leinize)))

(defn lein-source-paths [project-root merged-deps tdeps]
  (->> tdeps
       (filter-by-manifest :deps)
       (mapv leinize)
       (apply concat)
       (into (:paths merged-deps))
       (map (partial absolute-path project-root))))

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
  [{:keys [root] {:keys [resolve-aliases]} :lein-tools-deps/config :as project} deps]
   (let [args-map (deps/combine-aliases deps resolve-aliases)
         tdeps-map (deps/resolve-deps deps args-map)]
     (-> project
         (update :dependencies concat (lein-dependencies tdeps-map))
         (update :source-paths concat (lein-source-paths root deps tdeps-map)))))

(defn make-deps [{:keys [root] {:keys [config-files] :as config} :lein-tools-deps/config :as project}]
  (->> config-files
       (canonicalise-dep-locs config (:root project))
       (filter #(.exists %))
       read-all-deps
       (absolute-deps root)))

(defn apply-middleware [project]
  (let [deps (make-deps project)]
    (resolve-deps project deps)))

(def defunct-loc-keys #{:system :home})

(def valid-loc-keys #{:install :user :project})

(defn loc-or-string? [l]
  (or (valid-loc-keys l) (string? l)))

(defn resolve-dependencies-with-deps-edn
  "Inject relevant keys from deps.edn files into the leiningen project map
  while honoring other user-supplied config."
  [{{:keys [config-files] :as config} :lein-tools-deps/config :as project}]

  (when (some defunct-loc-keys config-files)
    (lein/warn "Your :lein-tools-deps/config :config-files contains defunct location keys please update to the supported ones" valid-loc-keys)
    (lein/exit 1))
  
  (cond
    (seq config-files)

    (if (every? loc-or-string? config-files)
      (apply-middleware project)
      (do (lein/warn  "Every element in :lein-tools-deps/config :config-files must either be a file-path string or one of the location keys" valid-loc-keys)
          (lein/exit 1)))
      
    (not (map? config))

    (do (lein/warn  ":lein-tools-deps/config must specify a configuration map.")
        (lein/exit 1))

    ;; pass through
    :else project))

(comment
  (read-all-deps (canonicalise-dep-locs "/Users/rick/repos/lein-tools-deps/example" [:install :user "deps.edn"]))

  (resolve-deps "/Users/rick/repos/lein-tools-deps/example"
                (canonicalise-dep-locs "/Users/rick/repos/lein-tools-deps/example" [:install :user :project]))

  (resolve-deps (canonicalise-dep-locs "/users/foo/proj" [:install :user "example/deps.edn" "foo"])))
