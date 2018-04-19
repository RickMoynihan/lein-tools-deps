(ns lein-tools-deps.plugin-test
  (:require [clojure.test :refer :all]
            [lein-tools-deps.plugin :as sut]
            [lein-tools-deps.plugin :as plugin]))

; The mere presence of this file means that `lein test` will trigger a compilation
; of lein-tools-deps.plugin and at least we can know if it builds successfully.

(deftest canonicalise-dep-refs-test
  (let [canonicalised-files (sut/canonicalise-dep-refs [:system :home "example/deps.edn"])]
    (is (every? #(instance? java.io.File %) canonicalised-files))
    (is (every? #(.exists %) canonicalised-files))
    (is (= 3 (count canonicalised-files)))))


(deftest resolve-deps
  (let [deps (sut/resolve-deps (sut/canonicalise-dep-refs [:system :home "example/deps.edn"]))]
    (is (map? deps))
    (is (every? #{'[org.clojure/clojure "1.9.0"]
                  '[criterium/criterium "0.4.4"]
                  '[org.clojure/tools.nrepl "0.2.12"]
                  '[org.clojure/spec.alpha "0.1.143"]
                  '[org.clojure/core.specs.alpha "0.1.24"]}
                (:dependencies deps)))))

