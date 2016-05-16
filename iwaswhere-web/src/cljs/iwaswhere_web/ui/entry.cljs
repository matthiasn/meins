(ns iwaswhere-web.ui.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [reagent.core :as rc]
            [cljsjs.moment]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [reagent.core :as r]))

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

(defn trash-icon
  "Renders a trash icon, which transforms into a warning button that needs to be clicked
  again for actual deletion. This label is a little to the right, so it can't be clicked
  accidentally, and disappears again within 5 seconds."
  [trash-fn]
  (let [clicked (r/atom false)
        guarded-trash-fn (fn [_ev]
                           (swap! clicked not)
                           (.setTimeout js/window #(reset! clicked false) 5000))]
    (fn [trash-entry]
      (if @clicked
        [:span.delete-warn {:on-click trash-fn} [:span.fa.fa-trash] "  confirm delete?"]
        [:span.fa.fa-trash-o.toggle.trash {:on-click guarded-trash-fn}]))))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry?]
  (let [local (rc/atom {:edit-mode (contains? (:tags entry) "#new-entry")})]
    (fn
      [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry?]
      (let [ts (:timestamp entry)
            map? (:latitude entry)
            show-map? (contains? (:show-maps-for store-snapshot) ts)
            toggle-map #(put-fn [:cmd/toggle {:timestamp ts :key :show-maps-for}])
            toggle-edit #(swap! local update-in [:edit-mode] not)
            trash-entry #(if new-entry?
                          (put-fn [:entry/remove-local {:timestamp ts}])
                          (put-fn [:cmd/trash {:timestamp ts}]))]
        [:div.entry
         [:div.entry-header
          [:a {:href (str "/#" (.format (js/moment ts) "YYYY-MM-DD")) :target "_blank"}
           [:span.timestamp (.format (js/moment ts) "dddd, MMMM Do YYYY")]]
          [:span.timestamp (.format (js/moment ts) ", h:mm a")]
          (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
          [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
          (when-not (:comment-for entry)
            [:span.fa.fa-comment-o.toggle {:on-click (h/new-entry-fn put-fn {:comment-for ts})}])
          (when (seq (:comments entry))
            [:span.fa.fa-comments.toggle {:class    (when-not @show-comments? "hidden-comments")
                                          :on-click #(swap! show-comments? not)}])
          (when-not (:comment-for entry)
            [:a {:href  (str "/#" ts) :target "_blank"} [:span.fa.fa-external-link.toggle]])
          [trash-icon trash-entry]]
         [hashtags-mentions-list entry]
         [l/leaflet-map entry (or show-map? show-all-maps?)]
         (if (or new-entry? (:edit-mode @local))
           [e/editable-md-render entry hashtags mentions put-fn toggle-edit new-entry?]
           [md/markdown-render entry show-tags?])
         [m/image-view entry]
         [m/audioplayer-view entry]
         [m/videoplayer-view entry]]))))

(defn entry-with-comments
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-all-comments? new-entry?]
  (let [show-comments? (r/atom true)]
    (fn [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-all-comments? new-entry?]
      [:div
       [journal-entry entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry?]
       (when (and @show-comments? show-all-comments?)
         [:div.comments
          (let [comments (:comments entry)]
            (for [comment (if show-pvt? comments (filter u/pvt-filter comments))]
              ^{:key (str "c" (:timestamp comment))}
              [journal-entry
               comment store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? false]))
          (for [comment (filter #(= (:comment-for %) (:timestamp entry)) (vals (:new-entries store-snapshot)))]
            ^{:key (str "c" (:timestamp comment))}
            [journal-entry
             comment store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? true])])])))
