(ns iwaswhere-web.misc-utils-test
  "Here, we test some helpter functions. These tests are written in cljc and
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

(deftest double-ts-to-long-test
  (testing "correctly converts number"
    (is (= 100000 (u/double-ts-to-long 100))))
  (testing "converted number is of correct type"
    (is (= (type (u/double-ts-to-long 100)) #?(:clj  java.lang.Long
                                             :cljs js/Number))))
  (testing "calling with other than number results in nil"
    (is (nil? (u/double-ts-to-long nil)))
    (is (nil? (u/double-ts-to-long "123")))))

(def completed-entry
  {:arrival-date        "2016-08-16 11:29:41 +0000"
   :departure-date      "2016-08-16 16:33:19 +0000"
   :tags                #{"#visit" "#import"}
   :departure-timestamp 1.471365199000049E9
   :arrival-timestamp   1.471346981391931E9
   :horizontal-accuracy 29.1
   :type                "visit"
   :longitude           10.0
   :latitude            53.0
   :device              "iPhone"
   :timestamp           1471346981391
   :md                  "Duration: 303.6m #visit"})

(deftest visit-timestamps-test
  (testing "entry with completed visit parsed correctly"
    (is (= {:arrival-ts   1471346981391
            :departure-ts 1471365199000}
           (u/visit-timestamps completed-entry))))
  (testing "entry without visit parsed correctly"
    (is (= {:arrival-ts   nil
            :departure-ts nil}
           (u/visit-timestamps (-> completed-entry
                                   (dissoc :arrival-timestamp)
                                   (dissoc :departure-timestamp))))))
  (testing "entry with incomplete visit parsed correctly"
    (is (= {:arrival-ts   1471346981391
            :departure-ts nil}
           (u/visit-timestamps (merge completed-entry
                                      {:departure-timestamp 64092211200}))))))
