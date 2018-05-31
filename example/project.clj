(defproject foo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :lein-tools-deps/config {:config-files [:install :user :project]} 
  
  :plugins [[lein-tools-deps "0.4.0-SNAPSHOT"]]

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  ;; Dependencies provided by deps.edn are merged with any found in
  ;; your configured leiningen profile. Normally we'd recommend you
  ;; remove as many dependencies as possible from your :dependencies
  ;; vector, unless you want them to be specific to your leiningen
  ;; environment. We include this sole dependency here to demonstrate
  ;; how deps.edn dependencies are combined with those in leiningen.
  :dependencies [[clj-time "0.14.4"]]

  :profiles {:cache {:lein-tools-deps/config {:resolve-aliases [:cache]}}}
  
  :aot [foo.core])