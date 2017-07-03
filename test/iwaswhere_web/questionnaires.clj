(ns iwaswhere-web.questionnaires
  (:require [clojure.test :refer :all]
            [iwaswhere-web.ui.questionnaires :as q]))

(def cfg
  {:aggregations {:pos {:type  :sum
                        :label "Positive Affect Score:"
                        :items #{1 3 5 9 10 12 14 16 17 19}}
                  :neg {:type  :sum
                        :label "Negative Affect Score:"
                        :items #{2 4 6 7 8 11 13 15 18 20}}}})

(def test-entry1
  {:questionnaires {:panas {7  3
                            20 2
                            1  1
                            4  3
                            15 4
                            13 3
                            6  3
                            17 3
                            3  3
                            12 2
                            2  2
                            19 3
                            11 4
                            9  2
                            5  4
                            14 3
                            16 4
                            10 3
                            18 3
                            8  3}}})
(def test-entry2
  {:questionnaires {:panas {7  3
                            20 2
                            1  1
                            4  3
                            15 4
                            13 3
                            6  3
                            17 3
                            3  3
                            12 2
                            2  2
                            19 3
                            11 4
                            9  2
                            5  4
                            14 3
                            16 4
                            10 3
                            18 3}}})

(deftest scores-test
  (testing "correct scores for completely filled questionnaire"
    (let [test-scores (q/scores test-entry1 [:questionnaires :panas] cfg)]
      (is (= 28 (-> test-scores :pos :score)))
      (is (= 30 (-> test-scores :neg :score)))))
  (testing "only calculates score for aggregation with all answers"
    (let [test-scores (q/scores test-entry2 [:questionnaires :panas] cfg)]
      (is (= 28 (-> test-scores :pos :score)))
      (is (nil? (:neg test-scores)))))
  (testing "returns empty map when no form data"
    (let [test-scores (q/scores {} [:questionnaires :panas] cfg)]
      (is (= {} test-scores)))))
