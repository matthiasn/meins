(ns iwaswhere-web.client-store-cfg-test
  (:require #?(:clj  [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer-macros [deftest testing is]])
            [iwaswhere-web.client-store-cfg :as cfg]))

(deftest layout-save-test
  (testing "layout is saved in cfg; save in localstorage initiated")
  (is (= {:new-state    {:cfg {:widgets {:all-stats     {:data-grid {:h 9
                                                                     :w 6
                                                                     :x 0
                                                                     :y 6}
                                                         :type      :all-stats-chart}
                                         :custom-fields {:data-grid {:h 6
                                                                     :w 6
                                                                     :x 0
                                                                     :y 0}
                                                         :type      :custom-fields-chart}
                                         :tabs-left     {:data-grid {:h 19
                                                                     :w 9
                                                                     :x 6
                                                                     :y 0}
                                                         :query-id  :left
                                                         :type      :tabs-view}
                                         :tabs-right    {:data-grid {:h 19
                                                                     :w 9
                                                                     :x 15
                                                                     :y 0}
                                                         :query-id  :right
                                                         :type      :tabs-view}}}}
          :send-to-self [:cfg/save]}
         (cfg/save-layout
           {:current-state {:cfg {:widgets {:tabs-left     {:type      :tabs-view
                                                            :query-id  :left
                                                            :data-grid {:x 6
                                                                        :y 0
                                                                        :w 9
                                                                        :h 19}}
                                            :tabs-right    {:type      :tabs-view
                                                            :query-id  :right
                                                            :data-grid {:x 15
                                                                        :y 0
                                                                        :w 9
                                                                        :h 19}}
                                            :custom-fields {:type      :custom-fields-chart
                                                            :data-grid {:x 0
                                                                        :y 0
                                                                        :w 6
                                                                        :h 10}}
                                            :all-stats     {:type      :all-stats-chart
                                                            :data-grid {:x 0
                                                                        :y 0
                                                                        :w 6
                                                                        :h 9}}}}}
            :msg-payload   [{:y 0
                             :w 9
                             :h 19
                             :x 6
                             :i "tabs-left"}
                            {:y 0
                             :w 9
                             :h 19
                             :x 15
                             :i "tabs-right"}
                            {:y 0
                             :w 6
                             :h 6
                             :x 0
                             :i "custom-fields"}
                            {:y 6
                             :w 6
                             :h 9
                             :x 0
                             :i "all-stats"}]}))))