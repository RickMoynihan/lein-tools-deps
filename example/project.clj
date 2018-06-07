(defproject foo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  
  :plugins [[lein-tools-deps "0.4.1-SNAPSHOT"]]

  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  ;; Dependencies provided by deps.edn are merged with any found in
  ;; your configured leiningen profile. Normally we'd recommend you
  ;; remove as many dependencies as possible from your :dependencies
  ;; vector, unless you want them to be specific to your leiningen
  ;; environment. We include this sole dependency here to demonstrate
  ;; how deps.edn dependencies are combined with those in leiningen.
  :dependencies [[clj-time "0.14.4"]]

  ;; Here we show how top level configurations can be merged with
  ;; configurations in profiles.
  ;;
  ;; The default project will include :deps along with :extra-deps
  ;; defined with the :async alias.
  :lein-tools-deps/config {:config-files [:install :user :project]
                           :resolve-aliases [:async]} 
  
  ;; You can configure lein-tools-deps to :resolve-aliases defined in
  ;; your deps.edn file.  You can do this, as demonstrated here,
  ;; by mapping groups of aliases into leiningen profiles.
  ;;
  ;; The active :profiles configuration are meta-merged together
  ;; before tools.deps resolution, and the combined set
  ;; of :resolve-aliases are given to tools.deps for it to resolve.
  ;;
  ;; So the :cache profile below will resolve-aliases for [:async :cache].
  :profiles {:resolve-alias-example {:lein-tools-deps/config {:resolve-aliases [:cache]}
                                     :main foo.resolve-alias}
             :classpath-alias-example {:lein-tools-deps/config {:classpath-aliases [:bench]}
                                       :main foo.classpath-alias}}
 

  :main foo.core
  
  )

