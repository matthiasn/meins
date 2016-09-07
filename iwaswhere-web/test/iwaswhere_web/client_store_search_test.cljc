(ns iwaswhere-web.client-store-search-test
  "Here, we test the search-related handler functions of the client side store
   component."
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
                    [iwaswhere-web.client-store :as store]
                    [iwaswhere-web.client-store-search :as search]
                    [iwaswhere-web.client-store-cfg :as c]
                    [iwaswhere-web.client-store-test :as st]))

(deftest update-query-test
  "Test that new query is updated properly in store component state"
  (let [current-state @(:state (store/initial-state-fn (fn [_put-fn])))
        handler-res (search/update-query-fn {:current-state current-state
                                             :msg-payload   st/empty-query})
        new-state (:new-state handler-res)
        toggle-msg {:timestamp (:timestamp st/test-entry) :query-id :query-1}
        new-state1 (:new-state (store/toggle-active-fn
                                 {:current-state new-state
                                  :msg-payload   toggle-msg}))
        new-state2 (:new-state (search/update-query-fn
                                 {:current-state new-state1
                                  :msg-payload   st/open-tasks-query}))]
    (testing
      "query is set locally"
      (is (= st/empty-query (-> new-state :query-cfg :queries :query-1))))
    (testing
      "query is sent, with additional :sort-by-upvotes key"
      (is (= (merge st/empty-query {:sort-by-upvotes nil
                                    :sort-asc        nil})
             (-> handler-res :emit-msg  second :queries :query-1))))
    (testing
      "active entry not set"
      (is (not (:active new-state))))
    (testing
      "active entry is set in base state for subseqent test"
      (is (= (:timestamp st/test-entry)
             (:query-1 (:active (:cfg new-state1))))))
    (testing
      "query is updated"
      (is (= st/open-tasks-query
             (-> new-state2 :query-cfg :queries :query-1))))
    (testing
      "active entry not set after updating query"
      (is (not (:active new-state2))))))

(deftest update-query-upvotes-test
  "Test that new query is sent properly, with :sort-by-upvotes set"
  (let [current-state @(:state (store/initial-state-fn (fn [_put-fn])))
        handler-res (search/update-query-fn {:current-state current-state
                                             :msg-payload   st/open-tasks-query})
        new-state (:new-state handler-res)
        new-state1 (:new-state (c/toggle-key-fn
                                 {:current-state new-state
                                  :msg-payload   {:path [:sort-by-upvotes]}}))
        handler-res1 (search/update-query-fn
                       {:current-state new-state1
                        :msg-payload   st/open-tasks-query})]
    (testing
      "query is set locally"
      (is (= st/open-tasks-query
             (-> new-state :query-cfg :queries :query-1))))
    (testing
      "query is sent, with additional but false :sort-by-upvotes key"
      (is (= (merge st/open-tasks-query {:sort-by-upvotes nil
                                         :sort-asc        nil})
             (-> handler-res :emit-msg  second :queries :query-1))))
    (testing
      "query is sent after upvotes-toggle, with additional :sort-by-upvotes key
       being true"
      (is (= (merge st/open-tasks-query {:sort-by-upvotes true
                                         :sort-asc        nil})
             (-> handler-res1 :emit-msg  second :queries :query-1))))))

(deftest show-more-test
  "Ensure that query is properly updated when more results are desired."
  (let [current-state @(:state (store/initial-state-fn (fn [_put-fn])))
        new-state (:new-state (search/update-query-fn
                                {:current-state current-state
                                 :msg-payload   st/open-tasks-query}))
        {:keys [send-to-self]} (search/show-more-fn
                                 {:current-state new-state
                                  :msg-payload   {:query-id :query-1}})
        updated-query (second send-to-self)
        expected-query (update-in st/open-tasks-query [:n] + 20)]
    (testing
      "send properly updated query, with increased number of results"
      (is (= updated-query expected-query)))))
