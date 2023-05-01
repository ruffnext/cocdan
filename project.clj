(defproject cocdan "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[buddy/buddy-auth "3.0.323"]
                 [buddy/buddy-core "1.10.413"]
                 [buddy/buddy-hashers "1.8.158"]
                 [buddy/buddy-sign "3.4.333"]
                 [ch.qos.logback/logback-classic "1.2.11"]
                 [clojure.java-time "0.3.3"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [com.cognitect/transit-cljs "0.8.280"]
                 [com.google.javascript/closure-compiler-unshaded "v20220803"]
                 [conman "0.9.5"]
                 [cprop "0.1.19"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"] 
                 [luminus-immutant "0.2.5"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.5"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.11.2"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.18"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [nrepl "0.9.0"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.webjars.npm/bulma "0.9.4"]
                 [org.webjars.npm/material-icons "1.0.0"]
                 [org.webjars/webjars-locator "0.45"]
                 [org.xerial/sqlite-jdbc "3.36.0.3"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.53"]
                 [reagent "1.1.1"]
                 [datascript "1.4.2"]
                 [re-posh "0.3.3"] 
                 [re-frame "1.3.0"]
                 [funcool/cats "2.4.2"]
                 [prismatic/schema "1.4.0"]
                 [com.taoensso/nippy "3.2.0"]
                 [io.github.ruffnext/cljs-http "0.1.47"] 
                 [metosin/malli "0.8.9"]
                 [tongue "0.4.4"]  ; i11n
                 [thheller/shadow-cljs "2.19.9" :scope "provided"]]

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
                  :dependencies [[binaryage/devtools "1.0.6"]
                                 [cider/piggieback "0.5.3"]
                                 [org.clojure/tools.namespace "1.3.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.9.5"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "1.2.4"]
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
