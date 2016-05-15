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

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments?]
  (let [local (rc/atom {:edit-mode (contains? (:tags entry) "#new-entry")})]
    (fn
      [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments?]
      (let [ts (:timestamp entry)
            map? (:latitude entry)
            show-map? (contains? (:show-maps-for store-snapshot) ts)
            toggle-map #(put-fn [:cmd/toggle {:timestamp ts :key :show-maps-for}])
            toggle-edit #(swap! local update-in [:edit-mode] not)
            trash-entry #(put-fn [:cmd/trash {:timestamp ts}])]
        [:div.entry
         [:div.entry-header
          [:span.timestamp (.format (js/moment ts) "MMMM Do YYYY, h:mm:ss a")]
          (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
          [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
          (when-not (:comment-for entry)
            [:span.fa.fa-comment-o.toggle {:on-click (h/new-entry-fn put-fn {:comment-for ts})}])
          [:span.fa.fa-trash-o.toggle {:on-click trash-entry}]
          (when (seq (:comments entry))
            [:span.fa.fa-comments.toggle {:class    (when-not @show-comments? "hidden-comments")
                                          :on-click #(swap! show-comments? not)}])
          (when-not (:comment-for entry)
            [:a {:href  (str "/#" ts) :target "_blank"}
             [:span.fa.fa-external-link.toggle]])]
         [hashtags-mentions-list entry]
         [l/leaflet-map entry (or show-map? show-all-maps?)]
         (if (:edit-mode @local)
           [e/editable-md-render entry hashtags mentions put-fn toggle-edit]
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
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt?]
  (let [show-comments? (r/atom true)]
    (fn [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt?]
      [:div
       [journal-entry entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments?]
       (when @show-comments?
         [:div.comments
          (let [comments (:comments entry)]
            (for [comment (if show-pvt? comments (filter u/pvt-filter comments))]
              ^{:key (str "c" (:timestamp comment))}
              [journal-entry comment store-snapshot hashtags mentions put-fn show-all-maps? show-tags?]))])])))
