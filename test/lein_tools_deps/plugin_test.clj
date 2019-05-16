(ns lein-tools-deps.plugin-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lein-tools-deps.plugin :as sut]
            [clojure.tools.deps.alpha.reader :as reader]
            [lein-tools-deps.env :as env])
  (:import (clojure.lang ExceptionInfo)))

; The mere presence of this file means that `lein test` will trigger a compilation
; of lein-tools-deps.plugin and at least we can know if it builds successfully.

(defn absolute-base-path []
  (.getAbsolutePath (io/file "")))

(def apply-middleware (partial sut/apply-middleware env/exists? reader/read-deps (env/clojure-env {})))

(deftest apply-middleware-to-source-paths
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:config-files ["test-cases/basic-deps.edn"]}})]
    (is (map? project))
    (is (= [(.getAbsolutePath (io/file (absolute-base-path) "src"))
            (.getAbsolutePath (io/file (absolute-base-path) "test"))]
           (:source-paths project)))))

;; TODO fix this test up properly.
#_(deftest resolve-local-root-to-source-paths
    (let [deps (sut/resolve-deps (absolute-base-path)
                                 (sut/canonicalise-dep-locs (absolute-base-path) ["test-cases/local-root-deps.edn"]))]
      (is (map? deps))
      (is (= ["src" "test"] (:source-paths deps)))))

(deftest apply-middleware-git-to-dependencies
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:config-files ["test-cases/git-deps.edn"]}})]
    (is (map? project))
    (let [dependencies (:dependencies project)]
      (is (>= (count dependencies) 2))
      (is (every? #{'[clj-time/clj-time "0.14.2"]
                    '[joda-time/joda-time "2.9.7"]}
                  dependencies)))))

(deftest apply-middleware-extra-deps
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:resolve-aliases [:bench]
                                                            :config-files    ["test-cases/alias-deps.edn"]}})]
    (is (map? project))
    (is (= (select-keys project [:dependencies :source-paths])
           {:dependencies [['criterium/criterium "0.4.4"]]
            :source-paths ()}))))

(deftest apply-middleware-extra-paths
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:classpath-aliases [:extra-paths-test]
                                                            :config-files      ["test-cases/alias-deps.edn"]}})]
    (is (map? project))
    (is (= (:source-paths project)
           ["test"]))))

(deftest apply-middleware-classpath-overrides
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:classpath-aliases [:classpath-overrides-test]
                                                            :config-files      ["test-cases/alias-deps.edn"]}
                                   :dependencies           [['org.clojure/clojure "1.9.0"]]})]
    (is (map? project))
    (is (= (:source-paths project)
           [(str (absolute-base-path) "/path/to/my/clojure")]))
    (is (empty? (:dependencies project)))))

(deftest apply-middleware-all-aliases
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:aliases      [:all]
                                                            :config-files ["test-cases/alias-deps.edn"]}
                                   :dependencies           [['org.clojure/clojure "1.9.0"]]})]
    (is (map? project))
    (is (= (:source-paths project)
           [(str (absolute-base-path) "/path/to/my/clojure")]))
    (is (= (:dependencies project)
           [['criterium/criterium "0.4.4"]]))))

(deftest resolve-dependencies-with-deps-edn-test
  (let [project {:lein-tools-deps/config {:config-files []}}]
    (is (= (sut/resolve-dependencies-with-deps-edn project)
           project)))
  (let [project {:lein-tools-deps/config {}}]
    (is (= (sut/resolve-dependencies-with-deps-edn project)
           project)))

  (let [project {:lein-tools-deps/config {:config-files [:bad-location]}}]
    (is (thrown? ExceptionInfo (sut/resolve-dependencies-with-deps-edn project))))
  (is (thrown? ExceptionInfo (sut/resolve-dependencies-with-deps-edn {}))))


(deftest apply-middleware-local-root
  (let [project (apply-middleware {:root                   (absolute-base-path)
                                   :lein-tools-deps/config {:aliases      [:test]
                                                            :config-files ["test-cases/local-root-issue.edn"]}
                                   :dependencies           [['org.clojure/clojure "1.9.0"]]})]
    (is (map? project))
    (is (= #{(str (absolute-base-path) "/test-cases/lib/dt.jar")
             (str (absolute-base-path) "/src")}
           (set (:source-paths project))))))
