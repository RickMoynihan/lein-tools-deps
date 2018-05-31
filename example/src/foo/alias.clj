(ns foo.alias
  (:require [clojure.core.cache :as cache])
  (:gen-class))

(defn -main [& _]
  (cache/fifo-cache-factory {:a 1 :b 2})
  (println "✓ Tested lein-tools-deps :resolve-aliases"))
