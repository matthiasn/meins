(ns iwaswhere-web.client-keepalive-test
  "Here, we test the keepalive handler functions."
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
            [matthiasn.systems-toolbox.component :as stc]
            [iwaswhere-web.client-store :as cs]
            [iwaswhere-web.keepalive :as k]))

(deftest frontend-keepalive-test
  "The keepalive mechanism consists of two parts:
    1) When a response to a :cmd/keep-alive message is received, the :last-alive timestamp is set
    2) Every so often, the scheduler sends a message to check if :last-alive timestamp is too long
       ago. In that case, we expect the component state to be reset to empty."
  (let [current-state @(:state (cs/initial-state-fn #()))
        new-state (:new-state (k/set-alive-fn {:current-state current-state}))]

    (testing ":last-alive timestamp set"
      (is (< (- (stc/now) (:last-alive new-state)) 100)))

    (testing "client state reset when :last-alive too long ago"
      (with-redefs [k/max-age -1]
        (let [new-state (:new-state (k/reset-fn {:current-state new-state}))]
          (is (empty? (:entries new-state)))
          (is (empty? (:entries-map new-state))))))))
