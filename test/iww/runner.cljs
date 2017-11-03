(ns iww.runner
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [iww.jvm.client-store-test]
            [iww.electron.renderer.client-store-entry-test]
            [iww.electron.renderer.client-store-search-test]
            [iww.jvm.ui-markdown-test]
            [iww.jvm.ui-pomodoros-test]
            [iww.jvm.parse-test]
            [iww.jvm.misc-utils-test]
            [clojure.string :as s]))

(enable-console-print!)

(defn tests []
  (run-tests 'iww.jvm.client-store-test
             'iww.electron.renderer.client-store-entry-test
             'iww.electron.renderer.client-store-search-test
             'iww.jvm.ui-markdown-test
             'iww.jvm.parse-test
             'iww.jvm.ui-pomodoros-test
             'iww.jvm.misc-utils-test))

(defn ^:export run []
  (tests)
  (let [res (with-out-str (tests))]
    (if (s/includes? res "0 failures, 0 errors") 0 1)))
