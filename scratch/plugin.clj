
(require '[lein-tools-deps.plugin :as pi] :reload)
(require '[clojure.tools.deps.alpha.reader :as reader] :reload)
(require '[clojure.tools.deps.alpha :as deps] :reload)

(def deps (map second pi/location->dep-paths))
(def all-deps (->> deps (filter #(.exists %))))

(def deps-map (reader/read-deps all-deps))
(def resolved (deps/resolve-deps deps-map {:verbose true}))
