(ns lein-tools-deps.lein-project
  (:require [clojure.tools.deps.alpha :as tools-deps]
            [clojure.tools.deps.alpha.util.maven :as maven-util]
            [lein-tools-deps.file-attributes :as file-attributes]))

(defmulti leinize (fn [[_dep-key dep-val]]
                    (:deps/manifest dep-val)))

(defmethod leinize :mvn [[artifact info]]
  (let [[group artifact-name classifier] (maven-util/lib->names artifact)
        properties (merge
                     (select-keys info
                                  [:extension
                                   :exclusions
                                   :scope])
                     (when (some? classifier)
                       {:classifier classifier}))]
    ;; Thanks to @seancorfield and boot-tools-deps for this snippet
    (transduce cat conj [(symbol group artifact-name) (:mvn/version info)]
               properties)))

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
       (map (partial file-attributes/absolute-path project-root))))

(defn make-classpath
  "Resolves additional classpaths, meta merging them into the lein project map"
  [{{:keys [classpath-aliases aliases]} :lein-tools-deps/config :as project} deps]
  (let [combined-aliases (tools-deps/combine-aliases deps (concat classpath-aliases aliases))
        classpath-overrides (:classpath-overrides combined-aliases)]
    (-> project
        (update :source-paths concat (:extra-paths combined-aliases))
        (update :source-paths concat (vals classpath-overrides))
        (update :dependencies (partial remove (fn [[sym _version]] (contains? classpath-overrides sym)))))))

(def ^:private tools-resolve-deps (memoize tools-deps/resolve-deps))

(defn resolve-deps
  "Resolves all dependencies, meta merging them into the lein project map"
  [{:keys [root] {:keys [resolve-aliases aliases]} :lein-tools-deps/config :as project} deps]
  (let [args-map (tools-deps/combine-aliases deps (concat resolve-aliases aliases))
        tdeps-map (tools-resolve-deps deps args-map)]
    (-> project
        (update :dependencies concat (lein-dependencies tdeps-map))
        (update :source-paths concat (lein-source-paths root deps tdeps-map)))))
