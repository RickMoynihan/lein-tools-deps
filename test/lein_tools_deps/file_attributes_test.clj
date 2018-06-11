(ns lein-tools-deps.file-attributes-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [lein-tools-deps.file-attributes :as sut])
  (:import (java.io File)))

(defn absolute-base-path []
  (.getAbsolutePath (io/file "")))

(deftest absolute-file-test
  (let [base-path (absolute-base-path)]
    (is (= (io/file base-path "deps.edn") (sut/absolute-file base-path "deps.edn")))
    (is (= (io/file base-path "foo" "deps.edn") (sut/absolute-file base-path (str "foo" File/separator "deps.edn"))))
    (let [abs-file (.getAbsoluteFile (io/file "some_dir" "deps.edn"))]
      (is (= abs-file (sut/absolute-file base-path abs-file))))))

