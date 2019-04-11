(ns lein-tools-deps.lein-project-test
  (:require [clojure.test :refer :all]
            [lein-tools-deps.lein-project :refer :all]))

(defn- deps-artifact [loc version & {:as properties}]
  [loc (merge {:mvn/version version
               :deps/manifest :mvn}
              properties)])

(defn- parse-lein-artifact [[artifact version & kvps]]
  (merge {:artifact artifact
          :version version}
         (into {} (map vec (partition 2 kvps)))))

(deftest leinize-test
  (testing "Artifact name only"
    (is (= ['artifact/artifact "1.01"] (leinize (deps-artifact 'artifact "1.01")))))

  (testing "Group and artifact name"
    (is (= ['group/artifact "4.3"] (leinize (deps-artifact 'group/artifact "4.3")))))

  (testing "With classifier"
    (is (= ['group/artifact "2.1" :classifier "native"] (leinize (deps-artifact 'group/artifact$native "2.1")))))

  (testing "With scope"
    (is (= {:artifact 'foo/bar
            :version "1.5"
            :scope "runtime"}
           (parse-lein-artifact (leinize (deps-artifact 'foo/bar "1.5" :scope "runtime"))))))

  (testing "With exclusions"
    (is (= {:artifact 'foo/bar
            :version "5.3"
            :exclusions '[baz quux]}
           (parse-lein-artifact (leinize (deps-artifact 'foo/bar "5.3" :exclusions '[baz quux]))))))

  (testing "With extension"
    (is (= {:artifact 'foo/bar
            :version "0.43"
            :extension "pom"}
           (parse-lein-artifact (leinize (deps-artifact 'foo/bar "0.43" :extension "pom"))))))

  (testing "All"
    (is (= {:artifact 'foo/bar
            :version "7.32"
            :classifier "native"
            :scope "dev"
            :exclusions ['excl/ude]
            :extension "pom"}
           (parse-lein-artifact (leinize (deps-artifact 'foo/bar$native "7.32"
                                                        :classifier "native"
                                                        :scope "dev"
                                                        :exclusions ['excl/ude]
                                                        :extension "pom")))))))
