(ns lein-tools-deps.lein-project
  (:require [clojure.tools.deps.alpha :as tools-deps]
            [lein-tools-deps.file-attributes :as file-attributes]))

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

(defn- upstream-exclusions
  "Given a tools.deps map of dependencies and a dependency entry from that map,
  return a seq containing all the upstream exclusions associated with that dependency"
  [full-deps [dep-name {:keys [dependents exclusions]}]]
  (-> exclusions
      (concat (map (fn [dependent-name]
                     (upstream-exclusions full-deps [dependent-name (get full-deps dependent-name)])) dependents))
      (flatten)))

(defn- with-upstream-exclusions
  "Modifies a tools.deps map of dependencies to have every dependeny explictly
  list all exclusions from its parents in the dependency tree"
  [tdeps-map]
  (reduce
   (fn [acc [dep-name _ :as dep]]
     (if-let [up-exc (seq (upstream-exclusions tdeps-map dep))]
       (assoc-in acc [dep-name :exclusions] up-exc)
       acc))
   tdeps-map
   tdeps-map))

(defn resolve-deps
  "Resolves all dependencies, meta merging them into the lein project map"
  [{:keys [root] {:keys [resolve-aliases aliases]} :lein-tools-deps/config :as project} deps]
  (let [args-map (tools-deps/combine-aliases deps (concat resolve-aliases aliases))
        tdeps-map (with-upstream-exclusions (tools-resolve-deps deps args-map))]
    (-> project
        (update :dependencies concat (lein-dependencies tdeps-map))
        (update :source-paths concat (lein-source-paths root deps tdeps-map)))))
