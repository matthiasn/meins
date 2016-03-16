(ns iwaswhere-web.journal
  (:require [markdown.core :as md]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :as h]
            [clojure.string :as s]
            [cljsjs.moment]
            [cljs.pprint :as pp]))

(defn w-geolocation
  [data pos]
  (let [coords (.-coords pos)
        latitude (.-latitude coords)
        longitude (.-longitude coords)]
    (merge data {:latitude  latitude
                 :longitude longitude
                 :timestamp (.-timestamp pos)})))

(defn send-w-geolocation
  [data put-fn]
  (let [geo (.-geolocation js/navigator)]
    (.getCurrentPosition geo (fn [pos]
                               (let [w-geoloc (w-geolocation data pos)]
                                 (pp/pprint w-geoloc)
                                 (put-fn [:geo-entry/persist w-geoloc]))))))

(defn markdown-render
  ""
  [md-string]
  [:div {:dangerouslySetInnerHTML {:__html (-> md-string (md/md->html md-string))}}])

(defn journal-view
  "Renders Journal div"
  [{:keys [observed local put-fn]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1-2
    [:div [:textarea {:type      "text"
                      :style     {:height (str (+ 20 (count (s/split-lines (:input @local)))) "em")}
                      :value     (:input @local)
                      :on-change #(swap! local assoc-in [:input] (.. % -target -value))}]]
    [:div [:button {:on-click (fn [_ev] (send-w-geolocation {:md (:input @local)} put-fn))} "save"]]]
   [:div.pure-u-1-2.markdown (markdown-render (:input @local))]
   [:div.pure-u-1-2
    [:h1 "Past entries"]
    [:hr]
    (for [entry (reverse (:entries @observed))]
      [:div
       (.format (js/moment (:timestamp entry)))
       (markdown-render (:md entry))
       [:hr]])]
   #_[:div.pure-u-sm-1 (h/pp-div @observed)]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))
