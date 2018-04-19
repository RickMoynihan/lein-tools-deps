(ns lein-tools-deps.plugin-test
  (:require [clojure.test :refer :all]
            [lein-tools-deps.plugin :as plugin]))

; The mere presence of this file means that `lein test` will trigger a compilation
; of lein-tools-deps.plugin and at least we can know if it builds successfully.

(deftest hello-travis
  (is (= :foo :foo)))
