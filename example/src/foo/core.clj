(ns foo.core
  (:require [criterium.core :as criterium]
            [clj-time.core :as clj-time]
            [demo :as demo]
            [local-root.core :as local-root])
  (:gen-class))

(defn -main
  "I don't do a whole lot."
  []
  (criterium/jvm-jit-name)
  (demo/tomorrow (clj-time/now))
  (local-root/local-root-resolves))