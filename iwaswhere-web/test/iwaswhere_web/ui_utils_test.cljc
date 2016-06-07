(ns iwaswhere-web.ui-utils-test
  "Here, we test the markdown UI functions. These tests are written in cljc and can also run on the JVM,
  as we only have pure punctions in the target namespace."
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
            [iwaswhere-web.ui.utils :as u]))

(deftest duration-string-test
  (testing "test output for some different durations"
    (is (= (u/duration-string 0) ""))
    (is (= (u/duration-string 11) "11s"))
    (is (= (u/duration-string 111) "1m 51s"))
    (is (= (u/duration-string 1111) "18m 31s"))
    (is (= (u/duration-string 11111) "3h 5m 11s"))
    (is (= (u/duration-string 111111) "30h 51m 51s"))))
