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
            [iwaswhere-web.utils.parse :as p]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def logo-img (js/require "./images/cljs.png"))


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
                :sente-opts  {:host "192.168.0.104:8765"}})

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn app-root []
  (let [greeting (subscribe [:get-greeting])
        stats (subscribe [:stats])]
    (fn []
      [view {:style {:flex-direction "column"
                     :margin         40
                     :align-items    "center"}}
       [text {:style {:font-size     30
                      :font-weight   "100"
                      :margin-bottom 20
                      :text-align    "center"}}
        @greeting]
       [text {:style {:font-size     30
                      :font-weight   "100"
                      :margin-bottom 20
                      :text-align    "center"}}
        (:open-tasks-cnt @stats) " open tasks"]
       [image {:source logo-img
               :style  {:width         80
                        :height        80
                        :margin-bottom 30}}]
       [touchable-highlight
        {:style    {:background-color "#999"
                    :padding          10
                    :border-radius    5}
         :on-press #(let [put-fn @ui/put-fn-atom
                          md (str "#test #task")
                          new-entry (p/parse-entry md)
                          new-entry-fn (h/new-entry-fn put-fn new-entry nil)]
                      (new-entry-fn)
                      (put-fn [:stats/get2]))}
        [text {:style {:color       "white"
                       :text-align  "center"
                       :font-weight "bold"}}
         "press me"]]])))

(defn init []
  (dispatch-sync [:initialize-db])
  (let [components #{(sente/cmp-map :app/ws-cmp sente-cfg)
                     (store/cmp-map :app/store-cmp)
                     (sched/cmp-map :app/scheduler-cmp)
                     (ui/cmp-map :app/ui-cmp)}
        components (make-observable components)]
    (prn components)
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
                            :app/ws-cmp}}]
       ]))
  )
