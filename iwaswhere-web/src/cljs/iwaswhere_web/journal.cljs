(ns iwaswhere-web.journal
  (:require [markdown.core :as md]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [clojure.string :as s]
            [clojure.set :as set]
            [iwaswhere-web.leaflet :as l]
            [cljsjs.moment]))

(defn hashtags-replacer
  "Replaces hashtags in entry text."
  [acc hashtag]
  (s/replace acc hashtag (str "**" hashtag "**")))

(defn mentions-replacer
  "Replaces mentions in entry text."
  [acc mention]
  (s/replace acc mention (str "**_" mention "_**")))

(defn- reducer
  "Generic reducer, allows calling specified function for each item in the collection."
  [text coll fun]
  (reduce fun text coll))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous here since
  application is only running locally, so in doubt we could only harm ourselves."
  [entry]
  (let [md-string (-> entry
                      :md
                      (reducer (:tags entry) hashtags-replacer)
                      (reducer (:mentions entry) mentions-replacer))]
    [:div {:dangerouslySetInnerHTML {:__html (md/md->html md-string)}}]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags in the new entry are contained in
  the filtered entry. This filters entries so that only entries that are relevant to the new
  entry are shown."
  ; TODO: also enable OR filter
  [new-entry]
  (fn [entry]
    (let [entry-tags (set (:tags entry))
          new-entry-tags (set (:tags new-entry))]
      (set/subset? new-entry-tags entry-tags))))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed]}]
  (let [store-snapshot @observed]
    [:div:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:hr]
      (let [entries (reverse (:entries store-snapshot))]
        (for [entry (take 50 (filter (entries-filter-fn (:new-entry store-snapshot)) entries))]
          ^{:key (:timestamp entry)}
          [:div.entry
           [:span.timestamp (.format (js/moment (:timestamp entry)) "MMMM Do YYYY, h:mm:ss a")]
           (markdown-render entry)
           (when-let [lat (:latitude entry)]
             [l/leaflet-component {:id  (str "map" (:timestamp entry))
                                   :lat lat
                                   :lon (:longitude entry)}])
           [:hr]]))]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))
