(defproject foo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :lein-tools-deps/config {:config-files [:install :user :project]} 
  
  :plugins [[lein-tools-deps "0.4.0-SNAPSHOT"]]
            
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn])
