(ns iwaswhere-web.store-test
  "Here, we test the handler functions of the server side store components."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.store :as s]
            [iwaswhere-web.graph :as g]
            [me.raynes.fs :as fs]))

(defn mk-test-entry
  "Generate test entry with current timestamp."
  []
  {:mentions #{"@myself"}
   :tags #{"#test" "#new"}
   :timezone "Europe/Berlin"
   :utc-offset -120
   :longitude 9.9832003
   :latitude 53.5652495
   :timestamp (System/currentTimeMillis)
   :md "Some #test #entry @myself "})

(def simple-query
  {:search-text "" :tags #{}
   :not-tags #{}
   :mentions #{}
   :date-string nil
   :timestamp nil
   :n 40})

(def test-daily-logs-path (str "./test-data/daily-logs/" (System/currentTimeMillis) "/"))
(fs/mkdir test-daily-logs-path)

(deftest geo-entry-persist-test
  "Validates that handler properly adds entry and persists entry, including storing the
  hashtags and mentions in graph."
  (with-redefs [f/daily-logs-path test-daily-logs-path]
    (let [test-entry (mk-test-entry)
          state (:state ((s/state-fn test-daily-logs-path) #()))
          {:keys [new-state emit-msg]} (f/geo-entry-persist-fn {:current-state @state
                                                                :msg-payload   test-entry})
          res (g/get-filtered-results new-state simple-query)]

      (testing "entry added to graph"
        (is (contains? (:sorted-entries new-state) (:timestamp test-entry)))
        ;; graph query also contains lists of comments and linked entries, which would be empty here
        (is (= test-entry (-> (first (:entries res))
                               (dissoc :comments)
                               (dissoc :linked-entries-list)))))

      (testing "hashtag was created for entry"
        (is (= (:tags test-entry) (:hashtags res))))

      (testing "mention was created for entry"
        (is (= (:mentions test-entry) (:mentions res))))

      (testing "log was appended by entry"
        (let [files (file-seq (clojure.java.io/file test-daily-logs-path))
              last-log (last (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn"))]
          (with-open [reader (clojure.java.io/reader last-log)]
            (let [last-line (last (line-seq reader))
                  parsed (clojure.edn/read-string last-line)]
              (is (= parsed test-entry))))))

      (testing "handler emits saved message"
        (is (= test-entry (second emit-msg)))))))
