(ns lein-tools-deps.env-test
  (:require [clojure.test :refer :all]
            [lein-tools-deps.env :refer :all]))

(deftest clojure-exe-test
  (testing "Default"
    (is (= "clojure" (clojure-exe {}))))

  (testing "Valid executable specified"
    (let [invalid "test/resources/missing"
          valid "test/resources/clojure-cmd"]
      (is (= valid (clojure-exe {:clojure-executables [invalid valid]})))))

  (testing "No valid executable"
    (let [candidates ["test/resources/bad-cmd"
                      "test/resources/missing"]]
      (try
        (clojure-exe {:clojure-executables candidates})
        (is false "Expected exception but none was thrown")
        (catch Exception ex
          (is (= {:tried-paths candidates} (ex-data ex))))))))
