(ns iwaswhere-web.runner
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [iwaswhere-web.client-store-test]
            [iwaswhere-web.client-store-entry-test]
            [iwaswhere-web.client-store-search-test]
            [iwaswhere-web.ui-markdown-test]
            [iwaswhere-web.ui-pomodoros-test]
            [iwaswhere-web.parse-test]
            [iwaswhere-web.misc-utils-test]
            [clojure.string :as s]))

(enable-console-print!)

(defn tests []
  (run-tests 'iwaswhere-web.client-store-test
             'iwaswhere-web.client-store-entry-test
             'iwaswhere-web.client-store-search-test
             'iwaswhere-web.ui-markdown-test
             'iwaswhere-web.parse-test
             'iwaswhere-web.ui-pomodoros-test
             'iwaswhere-web.misc-utils-test))

(defn ^:export run []
  (tests)
  (let [res (with-out-str (tests))]
    (if (s/includes? res "0 failures, 0 errors") 0 1)))
