(ns iwaswhere-web.store-query-test
  "Here, we test the handler functions of the server side store component."
  (:require [clojure.test :refer [deftest testing is]]
            [matthiasn.systems-toolbox.component :as stc]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.store :as s]
            [iwaswhere-web.store :as s]
            [clojure.set :as set]
            [iwaswhere-web.graph.stats :as gs]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.graph.stats :as gs]))

(def simple-query
  {:search-text ""
   :tags        #{}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           40
   :query-id    :query-1})

(def no-results-query
  {:search-text "some #not-existing-tag"
   :tags        #{"#not-existing-tag"}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           40})

(def tasks-query
  (merge simple-query
         {:search-text "#task"
          :tags        #{"#task"}}))

(def tasks-done-query
  (merge simple-query
         {:search-text "#task #done"
          :tags        #{"#task" "#done"}}))

(def tasks-not-done-query
  (merge simple-query
         {:search-text "#task ~#done ~#backlog"
          :tags        #{"#task"}
          :not-tags    #{"#done" "#backlog"}}))

(def test-entries
  [{:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450998000000
    :md        "Some #task"}
   {:mentions  #{}
    :tags      #{"#task"}
    :timestamp 1450998100000
    :md        "Some other #task"}
   {:mentions  #{}
    :tags      #{"#task" "#done"}
    :timestamp 1450998200000
    :md        "Some other #task #done"}
   {:mentions  #{}
    :tags      #{"#task" "#completed" "#done"}
    :timestamp 1450998300000
    :md        "Yet another completed #task - #done"}
   {:mentions  #{}
    :tags      #{"#task" "#completed" "#done"}
    :timestamp 1450998400000
    :md        "And yet another completed #task - #done"}])

(def private-tags #{"#pvt" "#private" "#nsfw" "#consumption"})

(defn persist-reducer
  "Reducing function for adding entries to component state."
  [acc entry]
  (:new-state (f/geo-entry-persist-fn {:current-state acc
                                       :msg-payload   entry})))

(defn extract-query-res
  "Extracts entries from query result as returned by publish-state-fn."
  [current-state client-id]
  (-> (s/publish-state-fn {:current-state current-state
                           :msg-payload   {:sente-uid client-id}})
      :emit-msg   ; get published messages
      first       ; 1st message
      second      ; msg-payload
      :entries))

(defn add-query
  "Adds query to state, returns new state."
  [current-state query client-id]
  (:new-state (s/state-get-fn {:current-state current-state
                               :msg-payload   query
                               :msg-meta      {:sente-uid client-id}})))

(deftest query-test
  "Test that different queries return the expected results."
  (let [test-ts (System/currentTimeMillis)
        {:keys [current-state logs-path]} (st/mk-test-state test-ts)]
    (with-redefs [f/daily-logs-path logs-path]
      (let [simple-query-uid (stc/make-uuid)
            simple-query2-uid (stc/make-uuid)
            no-results-query-uid (stc/make-uuid)
            tasks-query-uid (stc/make-uuid)
            tasks-done-query-uid (stc/make-uuid)
            tasks-not-done-query-uid (stc/make-uuid)

            new-state (reduce persist-reducer current-state test-entries)
            new-state (reduce persist-reducer new-state
                              (map st/mk-test-entry (range 100)))

            new-state (-> new-state
                          (add-query simple-query simple-query-uid)
                          (add-query (merge simple-query {:n 200}) simple-query2-uid)
                          (add-query tasks-query tasks-query-uid)
                          (add-query no-results-query no-results-query-uid)
                          (add-query tasks-done-query tasks-done-query-uid)
                          (add-query tasks-not-done-query tasks-not-done-query-uid))

            {:keys [new-state]} (gs/stats-tags-fn {:current-state new-state})
            client-queries (:client-queries new-state)]

        (testing
          "client queries associated with proper connection IDs"
          (is (= (get-in client-queries [simple-query-uid :queries :query-1])
                 simple-query))
          (is (= (get-in client-queries [tasks-done-query-uid :queries :query-1])
                 tasks-done-query)))

        (testing
          "client queries with not-tags properly re-formatted"
          (is (= (get-in client-queries
                         [tasks-not-done-query-uid :queries :query-1])
                 (merge tasks-not-done-query {:not-tags #{"#done" "#backlog"}}))))

        (testing
          "query with no matches should return 0 results"
          (is (empty? (extract-query-res new-state no-results-query-uid))))

        (testing
          "simple query has 40 results"
          (is (= 40 (count (extract-query-res new-state simple-query-uid)))))

        (testing
          "simple query2 returns all 105 results"
          (is (= 105 (count (extract-query-res new-state simple-query2-uid)))))

        (testing
          "tasks query has 5 results"
          (is (= 5 (count (extract-query-res new-state tasks-query-uid)))))

        (testing
          "tasks done query has 3 results"
          (is (= 3 (count (extract-query-res new-state tasks-done-query-uid)))))

        (testing
          "tasks - not done query has 2 results"
          (is (= 2 (count (extract-query-res new-state tasks-not-done-query-uid)))))

        (testing
          "stats show expected numbers"
          (let [res (gs/make-stats-tags new-state)
                stats (:stats res)]
            (is (= (:entry-count stats) 105))
            (is (= (:node-count stats) 122))))

        (testing
          "hashtags and mentions in result of stats-tags publish fn"
          (let [res (gs/make-stats-tags new-state)]
            (is (= (:hashtags res) #{"#task" "#entry" "#test" "#done"
                                     "#completed" "#blah" "#new"}))
            (is (= (:pvt-displayed res) private-tags))
            (is (= (:mentions res) #{"@myself" "@someone"}))))))))
