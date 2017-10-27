(ns i-was-where-app.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [i-was-where-app.events]
            [i-was-where-app.ui :as ui]
            [i-was-where-app.helpers :as h]
            [iwaswhere-web.client-store :as store]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [i-was-where-app.subs]
            [react-native :refer [TextInput View Text Image TouchableHighlight]]
            [iwaswhere-web.utils.parse :as p]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.pprint :as pp]))

(def text (r/adapt-react-class Text))
(def text-input (r/adapt-react-class TextInput))
(def view (r/adapt-react-class View))
(def image (r/adapt-react-class Image))
(def touchable-highlight (r/adapt-react-class TouchableHighlight))
(def logo-img (js/require "./images/icon.png"))
(def health-kit (js/require "rn-apple-healthkit"))
(def moment (js/require "moment"))
(def react-native-camera (js/require "react-native-camera"))
(def cam (r/adapt-react-class react-native-camera))
(def device-info (js/require "react-native-device-info"))

(def health-kit-opts
  (clj->js
    {:permissions {:read  ["Height" "Weight" "StepCount" "BodyMassIndex"
                           "FlightsClimbed"]
                   :write ["Weight" "StepCount" "BodyMassIndex"]}}))

(defn get-steps [days-ago put-fn local]
  (let [d (js/Date.)
        device-id (.getUniqueID device-info)
        _ (.setTime d (- (.getTime d) (* days-ago 24 60 60 1000)))
        opts (clj->js {:date (.toISOString d)})
        cb (fn [tag]
             (fn [err res]
               (let [res (js->clj res)
                     v (get-in res ["value"])
                     end-date (get-in res ["endDate"])]
                 (when v
                   (let [end-ts (.valueOf (moment end-date))
                         cnt (js/parseInt v)]
                     (put-fn [:entry/update
                              {:timestamp      end-ts
                               :md             (str cnt " " tag)
                               :tags           #{tag}
                               :vclock         {(str device-id) 1}
                               :linked-stories #{1475314976880}
                               :primary-story  1475314976880
                               :custom-fields  {tag {:cnt cnt}}}])))
                 (swap! local assoc-in [:steps] res))))
        init-cb (fn [err res]
                  (.getFlightsClimbed health-kit opts (cb "#flights-of-stairs"))
                  (.getStepCount health-kit opts (cb "#steps")))]
    (.initHealthKit health-kit health-kit-opts init-cb)))

(defn get-weight [put-fn local]
  (let [device-id (.getUniqueID device-info)
        weight-opts (clj->js {:unit      "gram"
                              :startDate (.toISOString (js/Date. 2015 9 1))})
        weight-cb (fn [err res]
                    (let [samples (js->clj res)]
                      (doseq [sample (js->clj res)]
                        (let [v (get-in sample ["value"])
                              end-date (get-in sample ["endDate"])
                              end-ts (.valueOf (moment end-date))
                              grams (js/parseInt v)
                              kg (/ grams 1000)]
                          (put-fn [:entry/update
                                   {:timestamp      end-ts
                                    :md             (str kg " #weight")
                                    :tags           #{"#weight"}
                                    :custom-fields  {"#weight" {:weight kg}}
                                    :vclock         {(str device-id) 1}
                                    :linked-stories #{1475314976880}
                                    :primary-story  1475314976880}])))))
        init-cb (fn [err res]
                  (.getWeightSamples health-kit weight-opts weight-cb))]
    (.initHealthKit health-kit health-kit-opts init-cb)))

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
                :sente-opts  {:host "192.168.178.21:8765"}})

(defn alert [title]
  (.alert (.-Alert react-native) title))

(defn app-root []
  (let [greeting (subscribe [:get-greeting])
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
                      :margin-bottom 10
                      :text-align    "center"}}
        (str device-id)]
       [image {:source logo-img
               :style  {:width         80
                        :height        80
                        :margin-bottom 5}}]
       ;[cam {}]
       [text-input {:style          {:height           250
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
                           new-entry (merge (p/parse-entry (:md @local))
                                            {:vclock {(str device-id) 1}})
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
                         (get-steps n put-fn local)))}
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
                       (get-weight put-fn local))}
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

       [text {:style {:font-size     30
                      :font-weight   "500"
                      :color         "#CCC"
                      :margin-bottom 20
                      :text-align    "center"}}
        (:open-tasks-cnt @stats) " open tasks "
        (str (:steps @local))]])))

(defn init []
  (dispatch-sync [:initialize-db])
  (let [components #{(sente/cmp-map :app/ws-cmp sente-cfg)
                     (store/cmp-map :app/store-cmp)
                     (sched/cmp-map :app/scheduler-cmp)
                     (ui/cmp-map :app/ui-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from #{:app/store-cmp
                            :app/ui-cmp}
                    :to   :app/ws-cmp}]

       [:cmd/route {:from #{:app/ws-cmp
                            :app/ui-cmp}
                    :to   :app/store-cmp}]

       [:cmd/route {:from #{:app/store-cmp
                            :app/ui-cmp}
                    :to   #{:app/scheduler-cmp}}]

       [:cmd/observe-state {:from :app/store-cmp
                            :to   :app/ui-cmp}]

       (when OBSERVER
         [:cmd/attach-to-firehose :app/ws-cmp])

       [:cmd/route {:from :app/scheduler-cmp
                    :to   #{:app/store-cmp
                            :app/ws-cmp}}]])))
