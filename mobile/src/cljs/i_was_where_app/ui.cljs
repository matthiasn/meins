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


(defn state-fn [put-fn]
  (let [
        ;app-root (app-root put-fn)
        ;register #(r/reactify-component app-root)
        ]
    ;(.registerComponent app-registry "iWasWhereApp" register)
    (reset! put-fn-atom put-fn))
  {:observed rdb/app-db})

(defn cmp-map [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
