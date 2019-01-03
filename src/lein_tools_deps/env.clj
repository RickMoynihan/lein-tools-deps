(ns lein-tools-deps.env
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io])
  (:import (java.io File)))

(defmulti exists? class)

(defmethod exists? String [file-path]
  (.exists (io/file file-path)))

(defmethod exists? File [file]
  (.exists file))

(def default-clojure-executables ["/usr/local/bin/clojure"
                                  "/usr/bin/clojure"])

(defn- clojure-exe
  [{:keys [clojure-executables]}]
  (let [clojure-paths (or clojure-executables default-clojure-executables)
        exe (->> clojure-paths
                 (filter exists?)
                 first)]
    (or exe (throw (ex-info "Could not find clojure executable" {:tried-paths clojure-paths})))))

(defn- scrape-clojure-env
  [{:keys [root] config :lein-tools-deps/config}]
  (shell/with-sh-dir
    root
    (let [exe (clojure-exe config)
          {:keys [out exit] :as result} (shell/sh exe "-Sdescribe")]
      (if (zero? exit)
        (read-string out)
        (throw (ex-info "Unable to locate Clojure's edn files" result))))))

(def clojure-env
  "Returns a map describing the environment known to clj/clojure:
  {:config-files [ ... ]}"
  (memoize scrape-clojure-env))

