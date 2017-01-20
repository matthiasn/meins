(ns iwaswhere-web.store-keepalive-test
  "Here, we test the keepalive handler functions."
  (:require [clojure.test :refer [deftest testing is]]
            [matthiasn.systems-toolbox.component :as stc]
            [iwaswhere-web.store :as s]
            [iwaswhere-web.graph.query :as gq]
            [iwaswhere-web.store-test :as st]
            [iwaswhere-web.keepalive :as k]))

(deftest backend-keepalive-test
  (testing
    "The keepalive mechanism works as follows:
     Connected clients send frequent :cmd/keep-alive messages. The handler
     responds, which is required for the client to not remove its state."
    (let [test-ts (stc/now)
          sente-uid (stc/make-uuid)
          current-state (:current-state (st/mk-test-state test-ts))
          msg-meta {:sente-uid sente-uid}
          w-query (:new-state
                    (gq/query-fn {:current-state current-state
                                  :msg-payload   st/simple-query
                                  :msg-meta      msg-meta}))
          {:keys [emit-msg]} (k/keepalive-fn
                               {:current-state w-query
                                :msg-meta      msg-meta})]
      (testing
        "handler responds with keep-alive-res message"
        (is (= emit-msg [:cmd/keep-alive-res]))))))
