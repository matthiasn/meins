(ns iwaswhere-web.parse-test
  "Here, we test the parsing functions."
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
                     [matthiasn.systems-toolbox.component :as stc]
                     [iwaswhere-web.utils.parse :as p]))

(def empty-entry
  {:mentions   #{}
   :tags       #{}
   :md         ""})

(def test-entry
  {:mentions   #{}
   :tags       #{"#cljc"}
   :md         "Moving to #cljc"})

(def test-entry2
  {:mentions #{"@myself"}
   :tags #{"#task" "#UI" "#pomodoros" "#interacting" "#events" "#pomodoro" "#timestamp"}
   :md "New #task: count the time spent #interacting with the #UI when no #pomodoro is running. Mouse-over and key #events should be a good indicator for that. When nothing happens longer than x, don't extend the current period of activity but rather close the last one at the last #timestamp and create a new period of activity. I like that. Not all work can possibly happen in #pomodoros, and it would be a waste of data to not capture that time. @myself "})

(def test-entry3
  {:mentions   #{"@myself" "@JohnDoe"}
   :tags       #{"#tag2" "#tag3"}
   :md         "#tag1 #tag2#tag3@JohnDoe@myself"})

(deftest parse-entry-test
  (testing "empty entry is parsed correctly"
    (is (= (p/parse-entry (:md empty-entry)) empty-entry)))
  (testing "test-entry is parsed correctly"
    (is (= (p/parse-entry (:md test-entry)) test-entry)))
  (testing "test-entry2 is parsed correctly"
    (is (= (p/parse-entry (:md test-entry2)) test-entry2)))
  (testing "test-entry3 is parsed correctly: tags and mentions don't need whitespace around them, # at
            beginning of line not parsed as that's reserved for markdown headlines."
    (is (= (p/parse-entry (:md test-entry3)) test-entry3))))


(def empty-search
  {:search-text ""
   :tags        #{}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           15})

(def open-tasks-search
  {:search-text "#task ~#done ~#backlog ~#outdated"
   :tags        #{"#task"}
   :not-tags #{"#backlog" "#done" "#outdated"}
   :mentions    #{}
   :date-string nil
   :timestamp   nil
   :n           15})

(def tasks-done-search
  {:search-text "#task #done @myself"
   :tags        #{"#task" "#done"}
   :not-tags    #{}
   :mentions    #{"@myself"}
   :date-string nil
   :timestamp   nil
   :n           15})

(def day-search
  {:search-text "2016-06-07 #task #done @myself"
   :tags        #{"#task" "#done"}
   :not-tags    #{}
   :mentions    #{"@myself"}
   :date-string "2016-06-07"
   :timestamp   nil
   :n           15})

(def timestamp-search
  {:search-text "1465325998053"
   :tags        #{}
   :not-tags    #{}
   :mentions    #{}
   :date-string nil
   :timestamp   "1465325998053"
   :n           15})

(deftest parse-search-test
  (testing "empty query is parsed correctly"
    (is (= (p/parse-search (:search-text empty-search)) empty-search)))

  (testing "open tasks query is parsed correctly"
    (is (= (p/parse-search (:search-text open-tasks-search)) open-tasks-search)))

  (testing "tasks done query is parsed correctly"
    (is (= (p/parse-search (:search-text tasks-done-search)) tasks-done-search)))

  (testing "day query is parsed correctly"
    (is (= (p/parse-search (:search-text day-search)) day-search)))

  (testing "timestamp query is parsed correctly"
    (is (= (p/parse-search (:search-text timestamp-search)) timestamp-search))))


(def tags #{"#task" "#goal" "#autocomplete" "#autosuggestion" "#Clojure" "#ClojureScript"})
(def mentions #{"@JohnDoe" "@myself" "@me"})

(deftest autocomplete-tags-test
  (testing "empty string before cursor returns zero filtered tags"
    (is (= (p/autocomplete-tags "" "(?!^) ?#" tags) ["" #{}])))

  (testing "Hashtag at end of string parsed correctly"
    (is (= (p/autocomplete-tags "some #task" "(?!^) ?#" tags) ["#task" #{"#task"}])))

  (testing "empty tags returns empty filtered tags list"
    (is (= (p/autocomplete-tags "some #task" "(?!^) ?#" #{}) ["#task" #{}])))

  (testing "partial tag correctly matched with multiple matches"
    (is (= (p/autocomplete-tags "some #auto" "(?!^) ?#" tags) ["#auto" #{"#autocomplete" "#autosuggestion"}])))

  (testing "not case sensitive"
    (is (= (p/autocomplete-tags "some #cloju" "(?!^) ?#" tags) ["#cloju" #{"#Clojure" "#ClojureScript"}])))

  (testing "also working with mentions"
    (is (= (p/autocomplete-tags "@m" "@" mentions) ["@m" #{"@me" "@myself"}]))))
