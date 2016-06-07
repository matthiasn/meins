(ns iwaswhere-web.ui-pomodoros-test
  "Here, we test the pomodoro UI functions. These tests are written in cljc and can also run on the JVM,
  as we only have pure punctions in the target namespace."
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
                    [iwaswhere-web.ui.pomodoro :as p]))

(def test-entries
  [{:timestamp 12345}
   {:timestamp 12346 :entry-type :pomodoro :planned-dur 1500 :completed-time 1000 :interruptions 0}
   {:timestamp 12347 :entry-type :pomodoro :planned-dur 1500 :completed-time 1500 :interruptions 0}
   {:timestamp 12348 :entry-type :pomodoro :planned-dur 1500 :completed-time 1500 :interruptions 0}
   {:timestamp 12348 :entry-type :pomodoro :planned-dur 1500 :completed-time 1500 :interruptions 2}
   {:timestamp 12348 :entry-type :pomodoro :planned-dur 1500 :completed-time 1000 :interruptions 1}])

(deftest pomodoro-stats-test
  "Test that the pomodoro-stats properly summarizes pomodoro stats."
  (testing "works on empty seq of entries"
    (is (= (p/pomodoro-stats []) {:pomodoros           0
                                  :completed-pomodoros 0
                                  :total-time          0
                                  :interruptions       0
                                  :interruptions-str   nil})))

  (testing "works on test entries"
    (is (= (p/pomodoro-stats (take 4 test-entries))
           {:pomodoros           3
            :completed-pomodoros 2
            :total-time          4000
            :interruptions       0
            :interruptions-str   nil})))

  (testing "works on test entries with interruptions"
    (is (= (p/pomodoro-stats test-entries)
           {:pomodoros           5
            :completed-pomodoros 3
            :total-time          6500
            :interruptions       3
            :interruptions-str   ". Interruptions: 3"}))))

(deftest pomodoro-stats-view-test
  "Test that the pomodoro-stats-view function properly formats the pomodoro stats view,
  with the correct number of formatted icons and summary string."
  (testing "works on empty seq of entries"
    (is (nil? (p/pomodoro-stats-view []))))

  (testing "works on first 4 test entries, with two completed and one incomplete icons"
    (is (= (p/pomodoro-stats-view (take 4 test-entries))
           [:span
            [:span
             [:span.fa.fa-clock-o.completed]
             [:span.fa.fa-clock-o.completed]]
            [:span [:span.fa.fa-clock-o.incomplete]]
            " 1h 6m 40s"])))

  (testing "works on test entries with interruptions, with 3 completed and 2 incomplete icons"
    (is (= (p/pomodoro-stats-view test-entries)
           [:span
            [:span
             [:span.fa.fa-clock-o.completed]
             [:span.fa.fa-clock-o.completed]
             [:span.fa.fa-clock-o.completed]]
            [:span [:span.fa.fa-clock-o.incomplete]
             [:span.fa.fa-clock-o.incomplete]]
            " 1h 48m 20s. Interruptions: 3"]))))

(deftest pomodoro-stats-str-test
  "Test that the pomodoro-stats-str function properly formats the pomodoro stats string."
  (testing "works on empty seq of entries"
    (is (nil? (p/pomodoro-stats-str []))))

  (testing "works on test entries"
    (is (= (p/pomodoro-stats-str (take 4 test-entries))
           "Pomodoros: 2 of 3 completed, 1h 6m 40s")))

  (testing "works on test entries with interruptions"
    (is (= (p/pomodoro-stats-str test-entries)
           "Pomodoros: 3 of 5 completed, 1h 48m 20s. Interruptions: 3"))))

(def empty-test-entry
  {:mentions   #{}
   :tags       #{"#cljc"}
   :timezone   "Europe/Berlin"
   :utc-offset -120
   :timestamp  1465059173965
   :md         "Moving to #cljc"})

(def test-entry
  {:mentions       #{}
   :tags           #{"#cljc"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :entry-type     :pomodoro
   :planned-dur    1500
   :interruptions  0
   :comment-for    1465059139281
   :completed-time 0
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

(def test-entry2
  {:mentions       #{}
   :tags           #{"#cljc"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :entry-type     :pomodoro
   :planned-dur    1500
   :interruptions  0
   :comment-for    1465059139281
   :completed-time 100
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

(def test-entry2a
  {:mentions         #{}
   :tags             #{"#cljc"}
   :timezone         "Europe/Berlin"
   :utc-offset       -120
   :entry-type       :pomodoro
   :pomodoro-running true
   :planned-dur      1500
   :interruptions    0
   :comment-for      1465059139281
   :completed-time   100
   :timestamp        1465059173965
   :md               "Moving to #cljc"})

(def test-entry3
  {:mentions       #{}
   :tags           #{"#cljc"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :entry-type     :pomodoro
   :planned-dur    1500
   :interruptions  0
   :comment-for    1465059139281
   :completed-time 1500
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

(deftest pomodoro-header-test
  (let [fake-start-fn #()]
    (testing "renders nothing when entry not of type :pomodoro"
      (is (= (p/pomodoro-header empty-test-entry fake-start-fn false) nil)))

    (testing "renders just the icon when not started"
      (is (= (p/pomodoro-header test-entry fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] nil nil])))

    (testing "renders just the icon when not started"
      (is (= (p/pomodoro-header test-entry fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] nil nil])))

    (testing "renders icon and duration when started"
      (is (= (p/pomodoro-header test-entry2 fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] [:span.dur "1m 40s"] nil])))

    (testing "renders icon, duration and start button in edit mode"
      (is (= (p/pomodoro-header test-entry2 fake-start-fn true)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] [:span.dur "1m 40s"]
              [:span.btn {:on-click fake-start-fn :class "start"}
               [:span.fa {:class "fa-play-circle-o"}] " start"]])))

    (testing "renders icon, duration and pause button in edit mode when running"
      (is (= (p/pomodoro-header test-entry2a fake-start-fn true)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] [:span.dur "1m 40s"]
              [:span.btn {:on-click fake-start-fn :class "stop"}
               [:span.fa {:class "fa-pause-circle-o"}] " pause"]])))

    (testing "renders completed icon and duration when completed"
      (is (= (p/pomodoro-header test-entry3 fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.completed] [:span.dur "25m"] nil])))

    (testing "renders icon, duration and no start button in edit mode when time is up"
      (is (= (p/pomodoro-header test-entry3 fake-start-fn true)
             [:div.pomodoro [:span.fa.fa-clock-o.completed] [:span.dur "25m"] nil])))))
