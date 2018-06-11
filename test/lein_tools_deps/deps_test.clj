(ns lein-tools-deps.deps-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lein-tools-deps.deps :as sut]
            [lein-tools-deps.env :as env]))

(defn absolute-base-path []
  (.getAbsolutePath (io/file "")))

(deftest canonicalise-dep-refs-test
  (let [canonicalised-files (sut/canonicalise-dep-locs (env/clojure-env {}) (absolute-base-path) [:install "test-cases/basic-deps.edn"])]
    (is (every? #(instance? java.io.File %) canonicalised-files))
    (is (every? #(.exists %) canonicalised-files))
    (is (= 2 (count canonicalised-files))
        ":system and supplied file == 2 files")))

(deftest absolute-local-root-coords-test
  (let [base-path (absolute-base-path)]
    (is (= {:local/root (.getAbsolutePath (io/file base-path "foo"))}
           (sut/absolute-local-root-coords {:local/root "foo"} base-path)))
    (is (= {:local/root base-path}
           (sut/absolute-local-root-coords {:local/root base-path} base-path)))))

(deftest absolute-coords-test
  (let [base-path (absolute-base-path)]
    (is (= {:mvn/version "1.0.0"} (sut/absolute-coords {:mvn/version "1.0.0"} base-path)))
    (is (= {:local/root (.getAbsolutePath (io/file base-path "foo"))}
           (sut/absolute-coords {:local/root "foo"} base-path)))))

(deftest absolute-deps-map-test
  (let [base-path (absolute-base-path)]
    (is (= {'some-lib  {:mvn/version "1.0.0"}
            'local-lib {:local/root (.getAbsolutePath (io/file base-path "foo"))}}
           (sut/absolute-deps-map {'some-lib  {:mvn/version "1.0.0"}
                                   'local-lib {:local/root "foo"}}
                                  base-path)))))

(deftest absolute-deps-test
  (let [base-path (absolute-base-path)]
    (is (= {:paths ["foo"]
            :deps  {'some-lib  {:mvn/version "1.0.0"}
                    'local-lib {:local/root (.getAbsolutePath (io/file base-path "foo"))}}
            :aliases {:test {:extra-deps {'some-lib2 {:mvn/version "1.1.0"}
                                          'local-lib2 {:local/root (.getAbsolutePath (io/file base-path "bar"))}}}
                      :bar {:main-opts ["foo" "bar"]}}}
           (sut/absolute-deps {:paths   ["foo"]
                               :deps    {'some-lib  {:mvn/version "1.0.0"}
                                         'local-lib {:local/root "foo"}}
                               :aliases {:test {:extra-deps {'some-lib2  {:mvn/version "1.1.0"}
                                                             'local-lib2 {:local/root "bar"}}}
                                         :bar  {:main-opts ["foo" "bar"]}}}
                              base-path)))))

