(set-env!
 :dependencies
 '[[org.clojure/clojure "1.9.0-alpha10"]

   ;; dev deps
   [adzerk/boot-test "1.1.2" :scope "test"]
   [org.clojure/test.check "0.9.0"]
   ]

 :source-paths
 #{"src" "test"})


(require '[adzerk.boot-test :refer [test]])
