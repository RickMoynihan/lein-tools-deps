(ns lein-tools-deps.env
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            clojure.string)
  (:import (java.io File)))

(defmulti exists? class)

(defmethod exists? String [file-path]
  (.exists (io/file file-path)))

(defmethod exists? File [file]
  (.exists file))

(defn clojure-exe
  "Finds the best candidate name for the clojure command. If :clojure-executables
   is mapped to a sequence of names to search, returns the first name that identifies
   a valid file name. If no executables are provided, the clojure command will be
   searched for on the PATH."
  [{:keys [clojure-executables]}]
  (if (seq clojure-executables)
    (let [exe (->> clojure-executables
                   (filter exists?)
                   first)]
      (or exe (throw (ex-info "Could not find clojure executable" {:tried-paths clojure-executables}))))
    "clojure"))

(defn- windows?
  []
  (clojure.string/includes? (System/getProperty "os.name") "Windows"))

(defn- cross-platform-sh
  [exe & args]
  (if (windows?)
    (let [{:keys [out] :as result} (apply shell/sh "powershell" "-c" exe args)]
      (assoc result :out (clojure.string/replace out "\\" "\\\\"))) ; escape backslashes found in windows paths
    (apply shell/sh exe args)))

(defn- scrape-clojure-env
  [{:keys [root] config :lein-tools-deps/config}]
  (shell/with-sh-dir
    root
    (let [exe (clojure-exe config)
          {:keys [out exit] :as result} (cross-platform-sh exe "-Sdescribe")]
      (if (zero? exit)
        (read-string out)
        (throw (ex-info "Unable to locate Clojure's edn files" result))))))

(def clojure-env
  "Returns a map describing the environment known to clj/clojure:
  {:config-files [ ... ]}"
  (memoize scrape-clojure-env))

