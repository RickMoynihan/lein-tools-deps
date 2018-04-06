
(require '[lein-tools-deps.plugin :as pi] :reload)
(require '[org.satta.glob :as glob] :reload)
(require '[clojure.java.io :as io] :reload)
(require '[clojure.pprint :as pp])

(def system-deps-file
  (first
   (filter #(and % (.exists %))
     (into [(io/file "/usr/local/lib/clojure/deps.edn")]
        (glob/glob "/usr/local/*/clojure/deps.edn")))))
(pp/pprint system-deps-file)

(pp/pprint (pi/system-deps-file))
