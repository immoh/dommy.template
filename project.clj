(defproject immoh/dommy.template "0.2.0-SNAPSHOT"
  :clojurescript? true
  :description "Clojurescript DOM templating"
  :url "https://github.com/immoh/dommy.template"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "0.3.2"]
            [com.cemerick/clojurescript.test "0.2.1"]
            [com.cemerick/austin "0.1.3"]]

  :hooks [leiningen.cljsbuild]

  :profiles {:dev {:dependencies [[crate "0.2.3"] ;; for perf test
                                  [com.cemerick/clojurescript.test "0.2.1"]]}}

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy" "clojars"]}

  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :incremental? true
           :compiler {:output-to "target/unit-test.js"
                      :optimizations :whitespace
                      :pretty-print true}}}
   :test-commands {"unit" ["phantomjs" :runner
                           "window.literal_js_was_evaluated=true"
                           "target/unit-test.js"]}})
