(defproject cocdan "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[cljs-http "0.1.46"]
                 [org.clojure/data.json "2.4.0"]
                 [funcool/cats "2.4.1"]
                 [cljs-ajax "0.8.4"]
                 [clojure.java-time "0.3.3"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [com.h2database/h2 "1.4.200"]
                 [conman "0.9.3"]
                 [cprop "0.1.19"]
                 [day8.re-frame/http-fx "0.2.3"]
                 [expound "0.8.10"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-immutant "0.2.5"]
                 [luminus-migrations "0.7.1"]
                 [luminus-transit "0.1.3"]
                 [markdown-clj "1.10.7"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [nrepl "0.9.0"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.896" :scope "provided"]
                 [org.clojure/core.async "1.5.644"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.2.3"]
                 [org.webjars.npm/bulma "0.9.3"]
                 [org.webjars.npm/fortawesome__fontawesome-free "5.15.4"]
                 [org.webjars.npm/material-icons "1.0.0"]
                 [org.webjars/webjars-locator "0.42"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [re-frame "1.3.0-rc2"]
                 [reagent "1.1.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.45"]
                 [thheller/shadow-cljs "2.16.7" :scope "provided"]
                 [datascript "1.3.10"]
                 [re-posh "0.3.3"]
                 [denistakeda/posh "0.5.9"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot cocdan.core

  :plugins [[lein-immutant "2.1.0"]] 
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]
  

  :profiles
  {:uberjar {:omit-source true
             
             :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
             :aot :all
             :uberjar-name "cocdan.jar"
             :source-paths ["env/prod/clj"  "env/prod/cljs" ]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[binaryage/devtools "1.0.4"]
                                 [cider/piggieback "0.5.3"]
                                 [org.clojure/tools.namespace "1.2.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [re-frisk "1.5.2"]
                                 [ring/ring-devel "1.9.5"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]] 
                  
                  
                  :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"] 
                  
                  
                  }
   :profiles/dev {}
   :profiles/test {}})
