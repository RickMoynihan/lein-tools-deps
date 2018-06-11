(ns foo.classpath-alias
  (:require [criterium.core :as criterium])
  (:gen-class))


(defn -main
      "I don't do a whole lot."
      []
      (criterium/foo)
      (println "✓ Tested lein-tools-deps :classpath-aliases"))
