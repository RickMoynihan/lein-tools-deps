(ns lein-tools-deps.file-attributes
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(defn ^File absolute-file
  "Takes an absolute base path and a potentially relative file and returns an
  absolute file, using the base to to form the absolute file if needed."
  [base-path path]
  (let [file (io/file path)]
    (if (not (.isAbsolute file))
      (io/file base-path file)
      file)))

(defn absolute-path
  "Takes an absolute base path and a potentially relative file and returns an
  absolute path (string), using the base to to form the absolute file if
  needed."
  [base-path path]
  (.getAbsolutePath (absolute-file base-path path)))

