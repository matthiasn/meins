(ns iwaswhere-web.stats-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.graph.stats :as gs]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test-common :as stc]
            [clojure.pprint :as pp]))

(def stats-test-entries
  [{:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450999100000
    :md        "Some #task"}

   {:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450999200000
    :md        "Some other #task"}

   {:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450999000000
    :md        "Some other #task"}
   {:mentions  #{}
    :tags      #{"#done"}
    :timestamp 1450999000001
    :comment-for 1450999000000
    :md        "and #done"}

   {:mentions  #{"@someone"}
    :tags      #{"#task"}
    :timestamp 1450999300000
    :md        "Yet another #task"}
   {:mentions  #{"@someone"}
    :tags      #{"#backlog"}
    :timestamp 1450999300001
    :comment-for 1450999300000
    :md        "for #backlog"}

   {:mentions  #{"@JaneDoe"}
    :tags      #{"#task"}
    :timestamp 1450999400000
    :md        "And yet another #task @JaneDoe"}
   {:mentions  #{"@JaneDoe"}
    :tags      #{"#closed"}
    :timestamp 145099940001
    :comment-for 1450999400000
    :md        "irrelevant #closed @JaneDoe"}])

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
            (is (= {:backlog-cnt    1
                    :closed-cnt     1
                    :completed-cnt  1
                    :open-tasks-cnt 2}
                   stats))))))))
