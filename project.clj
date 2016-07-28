(defproject weather "0.1.0-SNAPSHOT"
  :description "A weather web app"
  :url "https://github.com/not-raspberry/diffing-proxy/weather"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [ring-server "0.4.0"]
                 [reagent "0.6.0-rc"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [yogthos/config "0.8"]
                 [org.clojure/clojurescript "1.9.93"
                  :scope "provided"]
                 [org.clojure/java.jdbc "0.6.2-alpha2"]
                 [org.postgresql/postgresql "9.4.1209"]
                 [hikari-cp "1.7.2"]
                 [migratus "0.8.27"]
                 [clj-http "3.1.0"]
				 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [clj-time "0.12.0"]
                 [com.cognitect/transit-clj "0.8.288"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [ring-transit "0.1.6"]
                 [cljs-ajax "0.5.8"]
                 [com.layerware/hugsql "0.4.7"]
                 [org.slf4j/slf4j-log4j12 "1.7.21"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.1"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler weather.handler/app
         :uberwar-name "weather.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "weather.jar"

  :main weather.server

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/bootstrap-theme.min.css" "resources/public/css/bootstrap-theme.css"
    "resources/public/css/bootstrap.min.css" "resources/public/css/bootstrap.css"}}

  :cljsbuild
  {:builds {:min
            {:source-paths ["src/cljs" "env/prod/cljs"]
             :compiler
             {:output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/uberjar"
              :optimizations :advanced
              :pretty-print  false}}
            :app
            {:source-paths ["src/cljs" "env/dev/cljs"]
             :compiler
             {:main "weather.dev"
              :asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :source-map true
              :optimizations :none
              :pretty-print  true}}
            }
   }

  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                      ]
   :css-dirs ["resources/public/css"]
   :ring-handler weather.handler/app}

  :profiles {:dev {:repl-options {:init-ns weather.repl}

                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.5.0"]
                                  [prone "1.1.1"]
                                  [figwheel-sidecar "0.5.4-7"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                  [pjstadig/humane-test-output "0.8.0"]]

                   :source-paths ["env/dev/clj"]
                   :resource-paths ["env/dev/config"]
                   :plugins [[lein-figwheel "0.5.4-5"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :env {:dev true}}

             :test {:dependencies [[org.clojure/test.check "0.9.0"]]
                    :resource-paths ["env/test/config"]}
             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
