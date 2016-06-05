(ns iwaswhere-web.client-store-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.client-store :as store]))

(def empty-query
  {:search-text ""
   :tags        #{}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           40})

(def open-tasks-query
  {:search-text "#task ~#done "
   :tags        #{"#task"}
   :not-tags    #{"~#done"}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           40})

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

(def entry-update
  {:timestamp 1465059173965
   :md        "Moving to #cljc. Edited entry."})

(deftest update-query-test
  "Test that new query is updated properly in store component state"
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-query-fn {:current-state current-state
                                                      :msg-payload   empty-query}))
        new-state1 (:new-state (store/set-active-fn {:current-state new-state
                                                     :msg-payload   test-entry}))
        new-state2 (:new-state (store/update-query-fn {:current-state new-state1
                                                       :msg-payload   open-tasks-query}))]
    (testing "query is set locally"
      (is (= empty-query (:current-query new-state))))
    (testing "active entry not set"
      (is (not (:active new-state))))
    (testing "active entry is set in base state for subseqent test"
      (is (= test-entry (:active new-state1))))
    (testing "query is updated"
      (is (= open-tasks-query (:current-query new-state2))))
    (testing "active entry not set after updating query"
      (is (not (:active new-state2))))))

(deftest show-more-test
  "Ensure that query is properly updated when more results are desired."
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-query-fn {:current-state current-state
                                                      :msg-payload   open-tasks-query}))
        {:keys [:new-state emit-msg]} (store/show-more-fn {:current-state new-state})
        updated-query (update-in open-tasks-query [:n] + 20)]
    (testing "query is properly updated, with increased number of results"
      (is (= updated-query (:current-query new-state))))
    (testing "emits correct query message"
      (is (= :state/get (first emit-msg)))
      (is (= updated-query (second emit-msg))))))

(deftest update-local-test
  "Test that local entry is properly attached to state."
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
        new-state2 (:new-state (store/update-local-fn {:current-state new-state :msg-payload entry-update}))]
    (testing "pomodoro test entry in new-entries"
      (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
    (testing "entry update is merged with previous entry, thus allows omitting keys"
      (is (= (merge test-entry entry-update) (get-in new-state2 [:new-entries 1465059173965]))))))

(def pomodoro-inc-msg
  {:timestamp 1465059173965})

(deftest pomodoro-inc-test
  "Test the time increment handler for running pomodoros. Expectation is that the :completed-time key
  is incremented on every call."
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
        new-state1 (:new-state (store/pomodoro-inc-fn {:current-state new-state :msg-payload pomodoro-inc-msg}))
        new-state2 (:new-state (store/pomodoro-inc-fn {:current-state new-state1 :msg-payload pomodoro-inc-msg}))]
    (testing "pomodoro test entry in new-entries"
      (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
    (testing "time incremented"
      (is (= 1 (get-in new-state1 [:new-entries 1465059173965 :completed-time]))))
    (testing "time incremented"
      (is (= 2 (get-in new-state2 [:new-entries 1465059173965 :completed-time]))))))
