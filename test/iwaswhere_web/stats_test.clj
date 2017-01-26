(ns iwaswhere-web.stats-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.graph.stats :as gs]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test-common :as stc]
            [clojure.pprint :as pp]
            [iwaswhere-web.graph.add :as ga]
            [clj-time.core :as ct]
            [clj-time.coerce :as ctc]))

(def stats-test-entries
  [{:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450999100000
    :md        "Some #task"}

   {:mentions     #{}
    :tags         #{"#task"}
    :linked-story 1484076392371
    :timestamp    1450999200000
    :md           "Some other #task"}
   {:mentions       #{}
    :tags           #{"#done"}
    :entry-type     :pomodoro
    :completed-time 291
    :planned-dur    10000
    :timestamp      1450999200001
    :comment-for    1450999200000
    :md             "and #done"}

   {:mentions     #{}
    :tags         #{"#task"}
    :timestamp    1450999000000
    :linked-story 1484076392372
    :md           "Some other #task"}
   {:mentions       #{}
    :tags           #{"#done"}
    :entry-type     :pomodoro
    :completed-time 699
    :planned-dur    -1
    :timestamp      1450999000001
    :comment-for    1450999000000
    :md             "and #done"}

   {:mentions  #{"@someone"}
    :tags      #{"#task"}
    :timestamp 1450999300000
    :linked-story   1484076392372
    :md        "Yet another #task"}
   {:mentions    #{"@someone"}
    :tags        #{"#backlog"}
    :timestamp   1450999300001
    :comment-for 1450999300000
    :md          "for #backlog"}
   {:mentions       #{}
    :tags           #{"#progress"}
    :entry-type     :pomodoro
    :completed-time 200
    :planned-dur    -1
    :timestamp      1450999300002
    :comment-for    1450999300000
    :md             "some #progress"}

   {:mentions  #{"@JaneDoe"}
    :tags      #{"#task"}
    :timestamp 1450999300010
    :md        "And yet another #task @JaneDoe"}
   {:mentions    #{"@JaneDoe"}
    :tags        #{"#closed"}
    :timestamp   1450999300011
    :entry-type     :pomodoro
    :completed-time 111
    :planned-dur    -1
    :comment-for 1450999300010
    :md          "irrelevant #closed @JaneDoe"}])

(deftest summary-stats-test
  "test that daily summaries"
  (let [test-ts (System/currentTimeMillis)
        {:keys [current-state logs-path]} (st/mk-test-state test-ts)]
    (with-redefs [f/daily-logs-path logs-path]
      (let [new-state (reduce stc/persist-reducer
                              current-state
                              stats-test-entries)]

        (testing
          "task summary stats are correct"
          (let [stats (gs/task-summary-stats new-state)]
            (is (= {:backlog-cnt       1
                    :closed-cnt        1
                    :completed-cnt     2
                    :due-tasks-cnt     0
                    :open-tasks-cnt    1
                    :started-tasks-cnt 0}
                   stats))))))))

(deftest pomodoro-stats-test
  "test that daily summaries"
  (let [test-ts (System/currentTimeMillis)
        {:keys [current-state logs-path]} (st/mk-test-state test-ts)]
    (with-redefs [f/daily-logs-path logs-path
                  ga/local-dt (fn [entry] (ctc/from-long (:timestamp entry)))]
      (let [new-state (reduce stc/persist-reducer
                              current-state
                              stats-test-entries)]
        (testing
          "task summary stats are correct"
          (let [mapper (gs/pomodoro-mapper new-state)
                stats (mapper {:date-string "2015-12-24"})]
            (is (= ["2015-12-24"
                    {:completed   0
                     :date-string "2015-12-24"
                     :started     1
                     :total       4
                     :total-time  1301
                     :time-by-story {1484076392371 291
                                     1484076392372 899
                                     :no-story     111}}]
                   stats))))))))
