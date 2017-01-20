(ns iwaswhere-web.upload-test
  "Here, we test the handler functions of the upload component."
  (:require [clojure.test :refer [deftest testing is]]
            [iwaswhere-web.upload :as u]))

(deftest cmp-map
  (testing "cmp-map contains required keys"
    (let [cmp-id :server/ft-cmp
          cmp-map (u/cmp-map cmp-id)]
      (is (= (:cmp-id cmp-map) cmp-id))
      (is (fn? (:state-fn cmp-map))))))
