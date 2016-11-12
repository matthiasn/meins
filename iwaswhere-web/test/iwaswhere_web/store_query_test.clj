(ns iwaswhere-web.store-query-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [matthiasn.systems-toolbox.component :as comp]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.graph.stats :as gs]
            [iwaswhere-web.graph.query :as gq]
            [iwaswhere-web.store-test-common :as stc]
            [iwaswhere-web.files :as f]
            [clojure.pprint :as pp]))

(deftest query-test
  "Test that different queries return the expected results."
  (let [test-ts (System/currentTimeMillis)
        {:keys [current-state logs-path]} (st/mk-test-state test-ts)]
    (with-redefs [f/daily-logs-path logs-path]
      (let [simple-query-uid (comp/make-uuid)
            simple-query2-uid (comp/make-uuid)
            no-results-query-uid (comp/make-uuid)
            tasks-query-uid (comp/make-uuid)
            tasks-done-query-uid (comp/make-uuid)
            tasks-not-done-query-uid (comp/make-uuid)

            new-state (reduce stc/persist-reducer current-state stc/test-entries)
            new-state (reduce stc/persist-reducer new-state
                              (map st/mk-test-entry (range 100)))

            req-msg
            {:queries {simple-query-uid         stc/simple-query
                       simple-query2-uid        (merge stc/simple-query {:n 200})
                       tasks-query-uid          stc/tasks-query
                       no-results-query-uid     stc/no-results-query
                       tasks-done-query-uid     stc/tasks-done-query
                       tasks-not-done-query-uid stc/tasks-not-done-query}}

            res (second (:emit-msg (gq/query-fn {:current-state new-state
                                                 :msg-payload   req-msg})))]

        (testing
          "query with no matches should return 0 results"
          (is (empty? (get-in res [:entries no-results-query-uid]))))

        (testing
          "simple query has 40 results"
          (is (= 40
                 (count (get-in res [:entries simple-query-uid])))))

        (testing
          "simple query2 returns all 105 results"
          (is (= 106
                 (count (get-in res [:entries simple-query2-uid])))))

        (testing
          "tasks query has 5 results"
          (is (= 5
                 (count (get-in res [:entries tasks-query-uid])))))

        (testing
          "tasks done query has 3 results"
          (is (= 3
                 (count (get-in res [:entries tasks-done-query-uid])))))

        (testing
          "tasks - not done query has 2 results"
          (is (= 2
                 (count (get-in res [:entries tasks-not-done-query-uid])))))

        (testing
          "stats show expected numbers"
          (let [res (gs/make-stats-tags new-state)
                stats (:stats res)]
            (is (= 106
                   (:entry-count stats)))
            (is (= 126
                   (:node-count stats)))))

        (testing
          "hashtags and mentions in result of stats-tags publish fn"
          (let [res (gs/make-stats-tags new-state)]
            (is (= (set (:hashtags res)) #{"#task" "#entry" "#test" "#done"
                                           "#completed" "#blah" "#new"}))
            (is (= stc/private-tags
                   (:pvt-displayed res)))
            (is (= #{"@myself" "@someone"}
                   (:mentions res)))))))))
