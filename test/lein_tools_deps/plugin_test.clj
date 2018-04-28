(ns lein-tools-deps.plugin-test
  (:require [clojure.test :refer :all]
            [lein-tools-deps.plugin :as sut]
            [lein-tools-deps.plugin :as plugin]))

; The mere presence of this file means that `lein test` will trigger a compilation
; of lein-tools-deps.plugin and at least we can know if it builds successfully.

(deftest canonicalise-dep-refs-test
  (let [canonicalised-files (sut/canonicalise-dep-locs "/users/foo/proj" [:system "test-cases/basic-deps.edn"])]
    (is (every? #(instance? java.io.File %) canonicalised-files))
    (is (every? #(.exists %) canonicalised-files))
    (is (= 2 (count canonicalised-files))
        ":system and supplied file == 2 files")))

(deftest resolve-paths-to-source-paths
  (let [deps (sut/resolve-deps (sut/canonicalise-dep-locs "/users/foo/proj" ["test-cases/basic-deps.edn"]))]
    (is (map? deps))
    (is (= ["src" "test"] (:source-paths deps)))))

;; TODO fix this test up properly.
#_(deftest resolve-local-root-to-source-paths
  (let [deps (sut/resolve-deps (sut/canonicalise-dep-locs "/users/foo/proj" ["test-cases/local-root-deps.edn"]))]
    (is (map? deps))
    (is (= ["src" "test"] (:source-paths deps)))))

(deftest resolve-deps-git-to-dependencies
  (let [deps (sut/resolve-deps (sut/canonicalise-dep-locs "/users/foo/proj" ["test-cases/git-deps.edn"]))]
    (is (map? deps))
    (let [dependencies (:dependencies deps)]
      (is (>= (count dependencies) 2))
      (is (every? #{'[clj-time/clj-time "0.14.2"]
                    '[joda-time/joda-time "2.9.7"]}
                  dependencies)))))

