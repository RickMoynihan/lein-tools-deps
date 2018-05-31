(ns lein-tools-deps.plugin-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lein-tools-deps.plugin :as sut])
  (:import (java.io File)
           (clojure.lang ExceptionInfo)))

; The mere presence of this file means that `lein test` will trigger a compilation
; of lein-tools-deps.plugin and at least we can know if it builds successfully.

(defn absolute-base-path []
  (.getAbsolutePath (io/file "")))

(deftest canonicalise-dep-refs-test
  (let [canonicalised-files (sut/canonicalise-dep-locs {} (absolute-base-path) [:install "test-cases/basic-deps.edn"])]
    (is (every? #(instance? java.io.File %) canonicalised-files))
    (is (every? #(.exists %) canonicalised-files))
    (is (= 2 (count canonicalised-files))
        ":system and supplied file == 2 files")))

(deftest apply-middleware-to-source-paths
  (let [deps (sut/apply-middleware {:root (absolute-base-path)
                                    :lein-tools-deps/config {:config-files ["test-cases/basic-deps.edn"]}})]
    (is (map? deps))
    (is (= [(.getAbsolutePath (io/file (absolute-base-path) "src"))
            (.getAbsolutePath (io/file (absolute-base-path) "test"))]
          (:source-paths deps)))))

;; TODO fix this test up properly.
#_(deftest resolve-local-root-to-source-paths
  (let [deps (sut/resolve-deps (absolute-base-path)
               (sut/canonicalise-dep-locs (absolute-base-path) ["test-cases/local-root-deps.edn"]))]
    (is (map? deps))
    (is (= ["src" "test"] (:source-paths deps)))))

(deftest apply-middleware-git-to-dependencies
  (let [deps (sut/apply-middleware {:root (absolute-base-path)
                                    :lein-tools-deps/config {:config-files ["test-cases/git-deps.edn"]}})]
    (is (map? deps))
    (let [dependencies (:dependencies deps)]
      (is (>= (count dependencies) 2))
      (is (every? #{'[clj-time/clj-time "0.14.2"]
                    '[joda-time/joda-time "2.9.7"]}
                  dependencies)))))

(deftest apply-middleware-extra-paths
  (let [deps (sut/apply-middleware {:root (absolute-base-path)
                                    :lein-tools-deps/config {:resolve-aliases [:bench]
                                                             :config-files ["test-cases/alias-deps.edn"]}})]
    (is (map? deps))
    (is (= (select-keys deps [:dependencies :source-paths])
           {:dependencies [['criterium/criterium "0.4.4"]]
            :source-paths ()}))))

(deftest absolute-file-test
  (let [base-path (absolute-base-path)]
    (is (= (io/file base-path "deps.edn") (sut/absolute-file base-path "deps.edn")))
    (is (= (io/file base-path "foo" "deps.edn") (sut/absolute-file base-path (str "foo" File/separator "deps.edn"))))
    (let [abs-file (.getAbsoluteFile (io/file "some_dir" "deps.edn"))]
      (is (= abs-file (sut/absolute-file base-path abs-file))))))

(deftest absolute-local-root-coords-test
  (let [base-path (absolute-base-path)]
    (is (= {:local/root (.getAbsolutePath (io/file base-path "foo"))}
          (sut/absolute-local-root-coords base-path {:local/root "foo"})))
    (is (= {:local/root base-path}
          (sut/absolute-local-root-coords base-path {:local/root base-path})))))

(deftest absolute-coords-test
  (let [base-path (absolute-base-path)]
    (is (= {:mvn/version "1.0.0"} (sut/absolute-coords base-path {:mvn/version "1.0.0"})))
    (is (= {:local/root (.getAbsolutePath (io/file base-path "foo"))}
          (sut/absolute-coords base-path {:local/root "foo"})))))

(deftest absolute-deps-map-test
  (let [base-path (absolute-base-path)]
    (is (= {'some-lib  {:mvn/version "1.0.0"}
            'local-lib {:local/root (.getAbsolutePath (io/file base-path "foo"))}}
          (sut/absolute-deps-map base-path {'some-lib  {:mvn/version "1.0.0"}
                                            'local-lib {:local/root "foo"}})))))

(deftest absolute-deps-test
  (let [base-path (absolute-base-path)]
    (is (= {:paths ["foo"]
            :deps  {'some-lib  {:mvn/version "1.0.0"}
                    'local-lib {:local/root (.getAbsolutePath (io/file base-path "foo"))}}
            :aliases {:test {:extra-deps {'some-lib2 {:mvn/version "1.1.0"}
                                          'local-lib2 {:local/root (.getAbsolutePath (io/file base-path "bar"))}}}
                      :bar {:main-opts ["foo" "bar"]}}}
          (sut/absolute-deps base-path {:paths   ["foo"]
                                        :deps    {'some-lib  {:mvn/version "1.0.0"}
                                                  'local-lib {:local/root "foo"}}
                                        :aliases {:test {:extra-deps {'some-lib2  {:mvn/version "1.1.0"}
                                                                      'local-lib2 {:local/root "bar"}}}
                                                  :bar  {:main-opts ["foo" "bar"]}}})))))

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

(deftest apply-middleware-test
  (let [input-project {:root                   (absolute-base-path)
                       :lein-tools-deps/config {:config-files    ["test-cases/alias-deps.edn"]
                                                :resolve-aliases [:bench]}}]
    (is (= (sut/apply-middleware input-project)
           (merge
             input-project
             {:dependencies [['criterium/criterium "0.4.4"]]
              :source-paths ()})))))