(ns iwaswhere-web.store-query-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.graph :as g]
            [me.raynes.fs :as fs]
            [iwaswhere-web.files :as f]
            [clojure.set :as set]
            [iwaswhere-web.store :as s]
            [clojure.pprint :as pp]))

(def simple-query
  {:search-text ""
   :tags        #{}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           40})

(def task-done-query
  (merge simple-query
         {:search-text "#task #done"
          :tags        #{"#task" "#done"}}))

(def task-not-done-query
  (merge simple-query
         {:search-text "#task ~#done ~#backlog"
          :tags        #{"#task"}
          :not-tags    #{"~#done" "~#backlog"}}))

(deftest query-test
  ""
  (let [test-ts (System/currentTimeMillis)
        {:keys [current-state]} (st/mk-test-state test-ts)
        sente-uid1 (str (java.util.UUID/randomUUID))
        sente-uid2 (str (java.util.UUID/randomUUID))
        sente-uid3 (str (java.util.UUID/randomUUID))
        w-query-1 (:new-state (s/state-get-fn {:current-state current-state
                                               :msg-payload   simple-query
                                               :msg-meta      {:sente-uid sente-uid1}}))
        w-query-2 (:new-state (s/state-get-fn {:current-state w-query-1
                                               :msg-payload   task-done-query
                                               :msg-meta      {:sente-uid sente-uid2}}))
        w-query-3 (:new-state (s/state-get-fn {:current-state w-query-2
                                               :msg-payload   task-not-done-query
                                               :msg-meta      {:sente-uid sente-uid3}}))
        client-queries (:client-queries w-query-3)]
    (testing "client queries associated with proper connection IDs"
      (is (= (get client-queries sente-uid1) simple-query))
      (is (= (get client-queries sente-uid2) task-done-query)))
    (testing "client queries with not-tags properly re-formatted"
      (is (= (get client-queries sente-uid3)
             (merge task-not-done-query {:not-tags #{"#done" "#backlog"}}))))))
