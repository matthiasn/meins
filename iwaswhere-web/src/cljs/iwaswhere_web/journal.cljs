(ns iwaswhere-web.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.leaflet :as l]
            [iwaswhere-web.markdown :as m]
            [iwaswhere-web.image :as i]
            [clojure.set :as set]
            [clojure.string :as s]
            [cljsjs.moment]
            [iwaswhere-web.helpers :as h]))

(defn hashtags-mentions-list
  "Horizontally renders list with hashtags and mentions."
  [entry]
  (let [tags (:tags entry)
        mentions (:mentions entry)]
    [:div.pure-u-1
     [:div.hashtags
      (for [mention mentions]
        ^{:key (str "tag-" mention)}
        [:span.mention.float-left mention])
      (for [hashtag tags]
        ^{:key (str "tag-" hashtag)}
        [:span.hashtag.float-left hashtag])]]))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry temp-entry hashtags mentions put-fn show-map? editable? show-all-maps? show-tags?]
  (let [ts (:timestamp entry)
        map? (:latitude entry)
        toggle-map #(put-fn [:cmd/toggle {:timestamp ts :key :show-maps-for}])
        toggle-edit #(put-fn [:cmd/toggle {:timestamp ts :key :show-edit-for}])
        trash-entry #(put-fn [:cmd/trash {:timestamp ts}])
        save-fn #(put-fn [:text-entry/update temp-entry])]
    [:div.entry
     [:div.entry-header
      [:span.timestamp (.format (js/moment ts) "MMMM Do YYYY, h:mm:ss a")]
      (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
      [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
      [:span.fa.fa-trash-o.toggle {:on-click trash-entry}]
      (when (and temp-entry (not= entry temp-entry))
        [:span.not-saved {:on-click save-fn} [:span.fa.fa-floppy-o] "  click to save"])]
     [hashtags-mentions-list (or temp-entry entry)]
     [l/leaflet-map entry (or show-map? show-all-maps?)]
     [m/md-render entry temp-entry hashtags mentions put-fn editable? show-tags?]
     [i/image-view entry]
     [:hr]]))

(defn pvt-filter
  "Filter for entries that I consider private."
  [entry]
  (let [tags (set (map s/lower-case (:tags entry)))
        private-tags #{"#pvt" "#private" "#nsfw"}
        matched (set/intersection tags private-tags)]
    (empty? matched)))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        store-snapshot @observed
        show-entries (:show-entries local-snapshot)
        entries (take show-entries (:entries store-snapshot))
        hashtags (:hashtags store-snapshot)
        mentions (:mentions store-snapshot)
        show-all-maps? (:show-all-maps local-snapshot)
        toggle-all-maps #(swap! local update-in [:show-all-maps] not)
        show-tags? (:show-hashtags local-snapshot)
        toggle-tags #(swap! local update-in [:show-hashtags] not)
        show-context? (:show-context local-snapshot)
        toggle-context #(swap! local update-in [:show-context] not)
        show-pvt? (:show-pvt local-snapshot)
        toggle-pvt #(swap! local update-in [:show-pvt] not)]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:span.fa.toggle-map.pull-right {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}]
      [:span.fa.fa-hashtag.toggle-map.pull-right {:class (when-not show-tags? "inactive") :on-click toggle-tags}]
      [:span.fa.fa-eye.toggle-map.pull-right {:class (when-not show-context? "inactive") :on-click toggle-context}]
      [:span.fa.fa-user-secret.toggle-map.pull-right {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}]
      [:hr]
      (for [entry (if show-pvt? entries (filter pvt-filter entries))]
        (let [ts (:timestamp entry)
              temp-entry (get-in store-snapshot [:temp-entries ts])
              show-map? (contains? (:show-maps-for store-snapshot) ts)
              editable? (contains? (:show-edit-for store-snapshot) ts)]
          (when (or editable? show-context?)
            ^{:key (:timestamp entry)}
            [journal-entry entry temp-entry hashtags mentions put-fn show-map? editable? show-all-maps? show-tags?])))
      (when (and show-context? (seq entries))
        (let [show-more #(swap! local update-in [:show-entries] + 20)]
          [:div.pure-u-1.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div.pure-u-1 (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats) " edges, " (count hashtags) " hashtags, "
         (count mentions) " people"])]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id        cmp-id
              :initial-state {:show-entries  20
                              :show-all-maps false
                              :show-hashtags true
                              :show-context  true
                              :show-pvt      false}
              :view-fn       journal-view
              :dom-id        "journal"}))
