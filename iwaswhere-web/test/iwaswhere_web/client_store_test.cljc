(ns iwaswhere-web.client-store-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.store-test :as st]
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

(def state-from-backend
  {:entries [test-entry st/some-test-entry]
   :linked-entries-list []
   :hashtags            #{"#drama" "#hashtag" "#blah"}
   :mentions            #{"@myself" "@me" "@I"}
   :stats               {:entry-count 4118 :node-count 5636 :edge-count 28726}
   :duration-ms         19})

(def entry-update
  {:timestamp 1465059173965
   :md        "Moving to #cljc. Edited entry."})

(def entry-geo-update
  {:mentions   #{}
   :tags       #{}
   :timezone   "Europe/Berlin"
   :new-entry  true
   :utc-offset -120
   :longitude  9.9
   :latitude   53.5
   :timestamp  1465059173965
   :md         ""})

(deftest new-state-test
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/new-state-fn {:current-state current-state
                                                   :msg-payload   state-from-backend}))]
    (testing "entries are on new state"
      (is (= (:entries new-state) (:entries state-from-backend))))
    (testing "entries map is on new state"
      (is (= (:entries-map new-state)
             (into {} (map (fn [entry] [(:timestamp entry) entry]) (:entries state-from-backend))))))
    (testing "hashtags are on new state"
      (is (= (:hashtags (:cfg new-state)) (:hashtags state-from-backend))))
    (testing "mentions are on new state"
      (is (= (:mentions (:cfg new-state)) (:mentions state-from-backend))))
    (testing "stats are on new state"
      (is (= (:stats new-state) (:stats state-from-backend))))
    (testing "query duration is on new state"
      (is (= (:duration-ms new-state) (:duration-ms state-from-backend))))))

(deftest set-active-test
  "Test that active entry is updated properly in store component state"
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/set-active-fn {:current-state current-state
                                                    :msg-payload   test-entry}))]
    (testing "active entry is set"
      (is (= test-entry (:active new-state))))))

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

(deftest new-entry-test
  "Test that local entry is properly set in state."
  (with-redefs [store/new-entries-ls (atom {})]
    (let [current-state @(:state (store/initial-state-fn #()))
          new-state (:new-state (store/new-entry-fn {:current-state current-state :msg-payload test-entry}))]
      (testing "pomodoro test entry in new-entries"
        (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
      (testing "new entries atom properly updated - this would be backed by localstorage on client"
        (is (= (:new-entries new-state) @store/new-entries-ls))))))

(deftest update-local-test
  "Test that local entry is properly attached to state."
  (with-redefs [store/new-entries-ls (atom {})]
    (let [current-state @(:state (store/initial-state-fn #()))
          new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
          new-state2 (:new-state (store/update-local-fn {:current-state new-state :msg-payload entry-update}))]
      (testing "pomodoro test entry in new-entries"
        (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
      (testing "entry update is merged with previous entry, thus allows omitting keys"
        (is (= (merge test-entry entry-update) (get-in new-state2 [:new-entries 1465059173965]))))
      (testing "new entries atom properly updated - this would be backed by localstorage on client"
        (is (= (:new-entries new-state2) @store/new-entries-ls))))))

(deftest geo-enrich-test
  "Test that local entry is properly attached to state."
  (with-redefs [store/new-entries-ls (atom {})]
    (let [current-state @(:state (store/initial-state-fn #()))
          new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
          new-state2 (:new-state (store/geo-enrich-fn {:current-state new-state :msg-payload entry-geo-update}))]
      (testing "pomodoro test entry in new-entries"
        (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
      (testing "entry update is merged with previous entry, thus allows omitting keys"
        (is (= (merge entry-geo-update test-entry) (get-in new-state2 [:new-entries 1465059173965]))))
      (testing "new entries atom properly updated - this would be backed by localstorage on client"
        (is (= (:new-entries new-state2) @store/new-entries-ls))))))

(deftest remove-local-test
  "Test that local entry is properly removed from state after delete message."
  (with-redefs [store/new-entries-ls (atom {})]
    (let [current-state @(:state (store/initial-state-fn #()))
          new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
          new-state2 (:new-state (store/remove-local-fn {:current-state new-state :msg-payload entry-update}))]
      (testing "pomodoro test entry in new-entries"
        (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
      (testing "entry update is removed from component state"
        (is (not (get-in new-state2 [:new-entries 1465059173965]))))
      (testing "new entries atom properly updated - this would be backed by localstorage on client"
        (is (= (:new-entries new-state2) @store/new-entries-ls))))))

(deftest entry-saved-test
  "New entry removed after backend confirms save."
  (with-redefs [store/new-entries-ls (atom {})]
    (let [current-state @(:state (store/initial-state-fn #()))
          new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
          new-state2 (:new-state (store/entry-saved-fn {:current-state new-state :msg-payload entry-update}))]
      (testing "test entry in new-entries"
        (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
      (testing "entry update is removed from component state after receiving save confirmation"
        (is (not (get-in new-state2 [:new-entries 1465059173965]))))
      (testing "new entries atom properly updated - this would be backed by localstorage on client"
        (is (= (:new-entries new-state2) @store/new-entries-ls))))))

(def pomodoro-inc-msg
  {:timestamp 1465059173965})

(deftest pomodoro-inc-test
  "Test the time increment handler for running pomodoros. Expectation is that the :completed-time key
  is incremented on every call."
  (let [play-counter (atom {"ticking-clock" 0 "ringer" 0})]
    (with-redefs [store/new-entries-ls (atom {})
                  store/play-audio (fn [id] (swap! play-counter update-in [id] inc))]
      (let [current-state @(:state (store/initial-state-fn #()))
            new-state (:new-state (store/update-local-fn {:current-state current-state :msg-payload test-entry}))
            new-state1 (:new-state (store/pomodoro-inc-fn {:current-state new-state :msg-payload pomodoro-inc-msg}))
            new-state2 (:new-state (store/pomodoro-inc-fn {:current-state new-state1 :msg-payload pomodoro-inc-msg}))]
        (testing "pomodoro test entry in new-entries"
          (is (= test-entry (get-in new-state [:new-entries 1465059173965]))))
        (testing "time incremented"
          (is (= 1 (get-in new-state1 [:new-entries 1465059173965 :completed-time]))))
        (testing "time incremented"
          (is (= 2 (get-in new-state2 [:new-entries 1465059173965 :completed-time]))))
        (testing "new entries atom properly updated - this would be backed by localstorage on client"
          (is (= (:new-entries new-state2) @store/new-entries-ls)))
        (testing "tick was played twice"
          (is (= (get-in @play-counter ["ticking-clock"]) 2)))))))

(deftest toggle-key-test
  "toggle key messages flip boolean value"
  (let [current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/toggle-key-fn {:current-state current-state
                                                    :msg-payload   {:path [:cfg :sort-by-upvotes]}}))
        new-state2 (:new-state (store/toggle-key-fn {:current-state current-state
                                                     :msg-payload   {:path [:some :crazy :long :path]}}))]
    (testing "before receiving toggle-key msg, key is false, as per initial state"
      (is (not (:sort-by-upvotes (:cfg current-state)))))
    (testing "after receiving toggle-key msg, key is true"
      (is (:sort-by-upvotes (:cfg new-state))))
    (testing "previously unknown key is set to true. can be nested"
      (is (get-in new-state2 [:some :crazy :long :path])))))

(deftest toggle-set-test
  "toggle key messages flip boolean value"
  (let [test-ts 1465059139281
        path [:cfg :show-maps-for]
        current-state @(:state (store/initial-state-fn #()))
        new-state (:new-state (store/toggle-set-fn {:current-state current-state
                                                    :msg-payload   {:timestamp test-ts :path path}}))
        new-state1 (:new-state (store/toggle-set-fn {:current-state new-state
                                                     :msg-payload   {:timestamp test-ts :path path}}))]
    (testing "cfg set is initially empty"
      (is (empty? (:show-maps-for (:cfg current-state)))))
    (testing "set contains timestamp after initial toggle"
      (is (contains? (:show-maps-for (:cfg new-state)) test-ts)))
    (testing "timestamp removed from set after subsequent toggle"
      (is (not (contains? (:show-maps-for (:cfg new-state1)) 1465059139281))))))