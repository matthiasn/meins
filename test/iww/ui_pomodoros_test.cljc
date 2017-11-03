(ns iww.ui-pomodoros-test
  "Here, we test the pomodoro UI functions. These tests are written in cljc and
   can also run on the JVM, as we only have pure functions in the target
    namespace."
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
                    [iww.electron.renderer.ui.pomodoro :as p]))

(def test-entries
  [{:timestamp 12345}
   {:timestamp      12346
    :entry-type     :pomodoro
    :planned-dur    1500
    :completed-time 1000}
   {:timestamp      12347
    :entry-type     :pomodoro
    :planned-dur    1500
    :completed-time 1500}
   {:timestamp      12348
    :entry-type     :pomodoro
    :planned-dur    1500
    :completed-time 1500}
   {:timestamp      12349
    :entry-type     :pomodoro
    :planned-dur    1500
    :completed-time 1500}
   {:timestamp      12350
    :entry-type     :pomodoro
    :planned-dur    1500
    :completed-time 1000}])

(deftest pomodoro-stats-test
  "Test that the pomodoro-stats properly summarizes pomodoro stats."
  (testing "works on empty seq of entries"
    (is (= (p/pomodoro-stats []) {:pomodoros           0
                                  :completed-pomodoros 0
                                  :total-time          0})))

  (testing "works on test entries"
    (is (= (p/pomodoro-stats (take 4 test-entries))
           {:pomodoros           3
            :completed-pomodoros 2
            :total-time          4000})))

  (testing "works on test entries with interruptions"
    (is (= (p/pomodoro-stats test-entries)
           {:pomodoros           5
            :completed-pomodoros 3
            :total-time          6500}))))

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
   :comment-for    1465059139281
   :completed-time 1500
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

(def test-entry3a
  {:mentions       #{}
   :tags           #{"#cljc"}
   :timezone       "Europe/Berlin"
   :utc-offset     -120
   :entry-type     :pomodoro
   :planned-dur    1500
   :comment-for    1465059139281
   :completed-time 1500
   :timestamp      1465059173965
   :md             "Moving to #cljc"})

;; TODO: this should be revived

#_
(deftest pomodoro-header-test
  (let [fake-start-fn #()]
    (testing "renders nothing when entry not of type :pomodoro"
      (is (nil? (p/pomodoro-header empty-test-entry fake-start-fn false))))

    (testing "renders just the icon when not started"
      (is (= (p/pomodoro-header test-entry fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] nil [:span] nil])))

    (testing "renders just the icon when not started"
      (is (= (p/pomodoro-header test-entry fake-start-fn false)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete] nil [:span] nil])))

    (testing "renders icon and duration when started"
      (is (= (p/pomodoro-header test-entry2 fake-start-fn false)
             [:div.pomodoro
              [:span.fa.fa-clock-o.incomplete]
              [:span.dur "1m 40s"]
              [:span]
              nil])))

    (testing "renders icon, duration and start button in edit mode"
      (is (= (p/pomodoro-header test-entry2 fake-start-fn true)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete]
              [:span.dur "1m 40s"]
              [:span]
              [:span.btn {:on-click fake-start-fn :class "start"}
               [:span.fa {:class "fa-play-circle-o"}] "start"]])))

    (testing "renders icon, duration and pause button in edit mode when running"
      (is (= (p/pomodoro-header test-entry2a fake-start-fn true)
             [:div.pomodoro [:span.fa.fa-clock-o.incomplete]
              [:span.dur "1m 40s"]
              [:span]
              [:span.btn {:on-click fake-start-fn :class "stop"}
               [:span.fa {:class "fa-pause-circle-o"}] "pause"]])))

    (testing "renders completed icon and duration when completed"
      (is (= (p/pomodoro-header test-entry3 fake-start-fn false)
             [:div.pomodoro
              [:span.fa.fa-clock-o.completed]
              [:span.dur "25m"]
              [:span]
              nil])))

    (testing
      "renders completed icon and duration when completed, with interruptions"
      (is (= (p/pomodoro-header test-entry3a fake-start-fn false)
             [:div.pomodoro
              [:span.fa.fa-clock-o.completed]
              [:span.dur "25m"]
              [:span
               [:span.fa.fa-bolt]
               [:span.fa.fa-bolt]
               [:span.fa.fa-bolt]]
              nil])))

    (testing
      "renders icon, duration and no start button in edit mode when time is up"
      (is (= (p/pomodoro-header test-entry3 fake-start-fn true)
             [:div.pomodoro
              [:span.fa.fa-clock-o.completed]
              [:span.dur "25m"]
              [:span]
              nil])))

    (testing
      "renders icon, duration and no start button in edit mode when time is up"
      (is (= (p/pomodoro-header test-entry3a fake-start-fn true)
             [:div.pomodoro
              [:span.fa.fa-clock-o.completed]
              [:span.dur "25m"]
              [:span
               [:span.fa.fa-bolt]
               [:span.fa.fa-bolt]
               [:span.fa.fa-bolt]]
              nil])))

    (testing "renders icon, duration and no start button in edit mode when time
              is up. Shows one bolt plus count when more than 3 interruptions."
      (is (= (p/pomodoro-header (merge test-entry3a {:interruptions 4})
                                fake-start-fn true)
             [:div.pomodoro
              [:span.fa.fa-clock-o.completed]
              [:span.dur "25m"]
              [:span
               [:span.fa.fa-bolt]
               [:span.bolt-cnt 4]]
              nil])))))
