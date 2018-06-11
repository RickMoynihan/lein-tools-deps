(ns lein-tools-deps.plugin
  (:require [leiningen.core.main :as lein]
            [clojure.tools.deps.alpha.reader :as reader]
            [lein-tools-deps.deps :as deps]
            [lein-tools-deps.env :as env]
            [lein-tools-deps.lein-project :as lein-project]))

;; load extensions
(require 'clojure.tools.deps.alpha.extensions.deps)
(require 'clojure.tools.deps.alpha.extensions.git)
(require 'clojure.tools.deps.alpha.extensions.local)
(require 'clojure.tools.deps.alpha.extensions.maven)

(def ^:private long-running-process-warning
  (memoize #(lein/warn "If there are a lot of uncached dependencies this might take a while ...")))

(defn apply-middleware
  ([exists? reader env project]
   (long-running-process-warning)
   (let [deps (deps/make-deps exists? reader env project)]
     (-> project
         (lein-project/resolve-deps deps)
         (lein-project/make-classpath deps))))
  ([project]
    (apply-middleware env/exists? reader/read-deps (env/clojure-env project) project)))

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
