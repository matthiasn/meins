(ns i-was-where-app.ui
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [re-frame.db :as rdb]))

(defonce put-fn-atom (r/atom nil))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn app-root [put-fn]
  (fn []
    (let [greeting (subscribe [:get-greeting])]
      (fn []
        [view
         {:style {:flex-direction "column"
                  :margin         40
                  :align-items    "center"}}
         [text
          {:style {:font-size     30
                   :font-weight   "100"
                   :margin-bottom 20
                   :text-align    "center"}}
          @greeting]
         [image
          {:source logo-img
           :style  {:width         80
                    :height        80
                    :margin-bottom 30}}]
         [touchable-highlight
          {:style    {:background-color "#999"
                      :padding          10
                      :border-radius    5}
           :on-press #(do (alert "Yo12!")
                          (prn :yo12)
                          (put-fn [:stats/get2]))}
          [text
           {:style {:color       "white"
                    :text-align  "center"
                    :font-weight "bold"}}
           "tap me"]]]))))

(defn state-fn [put-fn]
  (let [app-root (app-root put-fn)
        register #(r/reactify-component app-root)]
    (.registerComponent app-registry "iWasWhereApp" register)
    (reset! put-fn-atom put-fn))
  {:observed rdb/app-db})

(defn cmp-map [cmp-id]
  (prn :ui-init)
  {:cmp-id   cmp-id
   :state-fn state-fn})
