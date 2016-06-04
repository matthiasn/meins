(ns iwaswhere-web.client-store-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.client-store :as store]))

(def pomodoro-test-entry
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

(deftest update-local-test
  "Test that local entry is properly attached to state."
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload pomodoro-test-entry}))]
    (testing "pomodoro test entry in new-entries"
      (is (= pomodoro-test-entry (get-in new-state [:new-entries 1465059173965]))))))

(def pomodoro-inc-msg
  {:timestamp 1465059173965})

(deftest pomodoro-inc-test
  "Test the time increment handler for running pomodoros. Expectation is that the :completed-time key
  is incremented on every call."
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload pomodoro-test-entry}))
        new-state1 (:new-state (store/pomodoro-inc-fn {:current-state new-state :msg-payload pomodoro-inc-msg}))
        new-state2 (:new-state (store/pomodoro-inc-fn {:current-state new-state1 :msg-payload pomodoro-inc-msg}))]
    (testing "pomodoro test entry in new-entries"
      (is (= pomodoro-test-entry (get-in new-state [:new-entries 1465059173965]))))
    (testing "time incremented"
      (is (= 1 (get-in new-state1 [:new-entries 1465059173965 :completed-time]))))
    (testing "time incremented"
      (is (= 2 (get-in new-state2 [:new-entries 1465059173965 :completed-time]))))))
