(ns i-was-where-app.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [i-was-where-app.events]
            [i-was-where-app.ios.healthkit :as hk]
            [i-was-where-app.ios.store :as store]
            [i-was-where-app.ui :as ui]
            [i-was-where-app.helpers :as h]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [i-was-where-app.subs]
            [iwaswhere-web.utils.parse :as p]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.pprint :as pp]))

(def ReactNative (js/require "react-native"))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))

(def logo-img (js/require "./images/icon.png"))
(def react-native-camera (js/require "react-native-camera"))
(def cam (r/adapt-react-class react-native-camera))
(def device-info (js/require "react-native-device-info"))

(defonce switchboard (sb/component :client/switchboard))

(def OBSERVER true)

(defn make-observable [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (prn "Attaching firehose")
      (set (mapv mapper components)))
    components))

(def sente-cfg {:relay-types #{:entry/update :entry/find :entry/trash
                               :import/geo :import/photos :import/phone
                               :import/spotify :import/flight :export/pdf
                               :stats/pomo-day-get :import/screenshot
                               :stats/get :stats/get2 :import/movie :blink/busy
                               :state/stats-tags-get :import/weight :import/listen
                               :state/search :cfg/refresh :firehose/cmp-recv
                               :firehose/cmp-put}
                :sente-opts  {:host "172.20.10.2:8765"}})

(defn app-root []
  (let [entries (subscribe [:entries])
        stats (subscribe [:stats])
        local (r/atom {:md "hello world"})
        device-id (.getUniqueID device-info)]
    (fn []
      [view {:style {:flex-direction   "column"
                     :padding-top      30
                     :padding-bottom   30
                     :padding-left     20
                     :padding-right    20
                     :height           "100%"
                     :background-color "#222"
                     :align-items      "center"}}
       [text {:style {:font-size     10
                      :color         :white
                      :font-weight   "100"
                      :margin-bottom 5
                      :text-align    "center"}}
        (str device-id " - " (count @entries) " entries")]
       ;[cam {}]
       [text-input {:style          {:height           200
                                     :font-weight      "100"
                                     :padding          10
                                     :font-size        20
                                     :background-color "#CCC"
                                     :width            "100%"}
                    :multiline      true
                    :default-value  (:md @local)
                    :keyboard-type  "twitter"
                    :on-change-text (fn [text]
                                      (swap! local assoc-in [:md] text))}]
       [view {:style {:flex-direction "row"
                      :padding-top    10
                      :padding-bottom 10
                      :padding-left   20
                      :padding-right  20}}
        [touchable-highlight
         {:style    {:background-color "green"
                     :padding-left     20
                     :padding-right    20
                     :padding-top      12
                     :padding-bottom   12
                     :margin-right     20}
          :on-press #(let [put-fn @ui/put-fn-atom
                           new-entry (p/parse-entry (:md @local))
                           new-entry-fn (h/new-entry-fn put-fn new-entry nil)]
                       (new-entry-fn)
                       (swap! local assoc-in [:md] ""))}
         [text {:style {:color       "white"
                        :text-align  "center"
                        :font-weight "bold"}}
          "new"]]
        [touchable-highlight
         {:style    {:background-color "blue"
                     :padding-left     20
                     :padding-right    20
                     :padding-top      12
                     :padding-bottom   12
                     :margin-right     20}
          :on-press #(let [put-fn @ui/put-fn-atom]
                       (dotimes [n 30]
                         (put-fn [:healthkit/steps n])))}
         [text {:style {:color       "white"
                        :text-align  "center"
                        :font-weight "bold"}}
          "steps"]]
        [touchable-highlight
         {:style    {:background-color "blue"
                     :padding-left     20
                     :padding-right    20
                     :padding-top      12
                     :padding-bottom   12
                     :margin-right     20}
          :on-press #(let [put-fn @ui/put-fn-atom]
                       (put-fn [:healthkit/weight]))}
         [text {:style {:color       "white"
                        :text-align  "center"
                        :font-weight "bold"}}
          "weight"]]
        [touchable-highlight
         {:style    {:background-color "#999"
                     :padding-left     20
                     :padding-right    20
                     :padding-top      12
                     :padding-bottom   12}
          :on-press #(let [put-fn @ui/put-fn-atom])}
         [text {:style {:color       "white"
                        :text-align  "center"
                        :font-weight "bold"}}
          "cam"]]]

       [text {:style {:font-size     10
                      :font-weight   "500"
                      :color         "#CCC"
                      :margin-bottom 20
                      :text-align    "center"}}
        (with-out-str (pp/pprint (second (last (sort-by first @entries)))))]
       [image {:source logo-img
               :style  {:width         80
                        :height        80
                        :margin-bottom 5}}]])))

(defn init []
  (dispatch-sync [:initialize-db])
  (let [components #{(sente/cmp-map :app/ws-cmp sente-cfg)
                     (store/cmp-map :app/store)
                     (hk/cmp-map :app/healthkit)
                     (sched/cmp-map :app/scheduler)
                     (ui/cmp-map :app/ui-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from #{:app/store
                            :app/healthkit
                            :app/ui-cmp}
                    :to   :app/ws-cmp}]

       [:cmd/route {:from #{:app/ws-cmp
                            :app/healthkit
                            :app/ui-cmp}
                    :to   :app/store}]

       [:cmd/route {:from #{:app/ws-cmp
                            :app/ui-cmp}
                    :to   :app/store}]

       [:cmd/route {:from :app/ui-cmp
                    :to   :app/healthkit}]

       [:cmd/observe-state {:from :app/store
                            :to   :app/ui-cmp}]

       (when OBSERVER
         [:cmd/attach-to-firehose :app/ws-cmp])

       [:cmd/route {:from :app/scheduler
                    :to   #{:app/store
                            :app/ws-cmp}}]])))
