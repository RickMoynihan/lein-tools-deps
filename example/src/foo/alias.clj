(ns foo.alias
  (:require [clojure.core.cache :as cache]
            [clojure.core.async :as async])
  (:gen-class))

(defn -main [& _]
  (cache/fifo-cache-factory {:a 1 :b 2})
  (async/chan)
  (println "✓ Tested lein-tools-deps :resolve-aliases"))
