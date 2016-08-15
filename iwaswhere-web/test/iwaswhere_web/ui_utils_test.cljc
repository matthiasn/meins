(ns iwaswhere-web.ui-utils-test
  "Here, we test the markdown UI functions. These tests are written in cljc and
   can also run on the JVM, as we only have pure punctions in the target
   namespace."
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
            [iwaswhere-web.utils.misc :as u]))

(deftest duration-string-test
  (testing "test output for some different durations"
    (is (= (u/duration-string 0) ""))
    (is (= (u/duration-string 11) "11s"))
    (is (= (u/duration-string 111) "1m 51s"))
    (is (= (u/duration-string 1111) "18m 31s"))
    (is (= (u/duration-string 11111) "3h 5m"))
    (is (= (u/duration-string 111111) "30h 51m"))))

(def test-entry
  {:mentions       #{}
   :tags           #{}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :timestamp      1465059173965
   :md             ""})

(def test-entry2
  {:mentions       #{}
   :tags           #{"#cljc"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

(def pvt-entry
  {:mentions       #{}
   :tags           #{"#pvt"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :timestamp      1465059173965
   :md             "Some #pvt entry"})

(def pvt-entry2
  {:mentions       #{}
   :tags           #{"#private"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :timestamp      1465059173965
   :md             "Some #private entry"})

(def pvt-entry3
  {:mentions       #{}
   :tags           #{"#nsfw"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :timestamp      1465059173965
   :md             "Something #nsfw"})

(deftest pvt-filter-test
  (testing "properly detects privacy status of entries"
    (is (u/pvt-filter test-entry))
    (is (u/pvt-filter test-entry2))
    (is (not (u/pvt-filter pvt-entry)))
    (is (not (u/pvt-filter pvt-entry2)))
    (is (not (u/pvt-filter pvt-entry3)))))
