(ns iwaswhere-web.ui.entry.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.ui.pomodoro :as p]
            [iwaswhere-web.utils.parse :as up]
            [iwaswhere-web.ui.entry.actions :as a]
            [iwaswhere-web.ui.entry.capture :as c]
            [iwaswhere-web.ui.entry.story :as es]
            [iwaswhere-web.ui.entry.thumbnails :as t]
            [cljsjs.moment]
            [iwaswhere-web.utils.misc :as u]
            [cljs.pprint :as pp]
            [clojure.set :as set]
            [iwaswhere-web.helpers :as h]))

(defn hashtags-mentions-list
  "Horizontally renders list with hashtags and mentions."
  [entry cfg tab-group put-fn]
  [:div.hashtags
   (when (:redacted cfg) {:class "redacted"})
   (for [mention (:mentions entry)]
     ^{:key (str "tag-" mention)}
     [:span.mention {:on-click (up/add-search mention tab-group put-fn)}
      mention])
   (for [hashtag (:tags entry)]
     ^{:key (str "tag-" hashtag)}
     [:span.hashtag {:on-click (up/add-search hashtag tab-group put-fn)}
      hashtag])])

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry cfg put-fn edit-mode? info local-cfg always-show-map? linked-desc]
  (let [ts (:timestamp entry)
        show-map? (contains? (:show-maps-for cfg) ts)
        toggle-edit #(if edit-mode? (put-fn [:entry/remove-local entry])
                                    (put-fn [:entry/update-local entry]))
        show-pvt? (:show-pvt cfg)
        hashtags (set/union (:hashtags cfg) (:pvt-displayed cfg))
        pvt-hashtags (:pvt-hashtags cfg)
        hashtags (if show-pvt? (concat hashtags pvt-hashtags) hashtags)
        mentions (:mentions cfg)
        q-date-string (.format (js/moment ts) "YYYY-MM-DD")
        tab-group (:tab-group local-cfg)]
    [:div.entry {:on-drop       (a/drop-linked-fn entry cfg put-fn)
                 :on-drag-over  h/prevent-default
                 :on-drag-enter h/prevent-default}
     [es/story-select entry cfg put-fn edit-mode?]
     [:div.header
      [:div
       [:a [:time {:on-click (up/add-search q-date-string tab-group put-fn)}
            (.format (js/moment ts) "ddd YY-MM-DD HH:mm")]]
       [:time (u/visit-duration entry)]]
      (if (= :pomodoro (:entry-type entry))
        [p/pomodoro-header entry #(put-fn [:cmd/pomodoro-start entry]) edit-mode?]
        [:div info])
      [:div
       (when (seq (:linked-entries-list entry))
         (let [ts (:timestamp entry)
               entry-active? (when-let [query-id (:query-id local-cfg)]
                               (= (query-id (:active cfg)) ts))
               set-active-fn #(put-fn [:cmd/toggle-active
                                       {:timestamp ts
                                        :query-id (:query-id local-cfg)}])]
           [:span.link-btn {:on-click set-active-fn
                            :class    (when entry-active? "active")}
            (str " linked: " (count (:linked-entries-list entry)))]))]
      [a/entry-actions entry cfg put-fn edit-mode? toggle-edit local-cfg]]
     [hashtags-mentions-list entry cfg tab-group put-fn]
     [es/story-name entry put-fn]
     (if edit-mode?
       [e/editable-md-render entry hashtags mentions put-fn toggle-edit]
       (if (and (empty? (:md entry)) linked-desc)
         [md/markdown-render
          (update-in linked-desc [:md]
                     #(str % " <span class=\"fa fa-link\"></span>"))
          cfg #()]
         [md/markdown-render entry cfg toggle-edit]))
     [c/activity-div entry cfg put-fn edit-mode?]
     [c/sleep-div entry put-fn edit-mode?]
     (when show-pvt?
       [c/consumption-div entry cfg put-fn edit-mode?])
     [c/custom-fields-div entry cfg put-fn edit-mode?]
     [m/audioplayer-view entry]
     [l/leaflet-map
      entry (or show-map? (:show-all-maps cfg) always-show-map?) local-cfg]
     [m/image-view entry]
     [m/videoplayer-view entry]
     [:div.footer
      [:div.likes (when-let [upvotes (:upvotes entry)]
              (when (pos? upvotes)
                [:div
                 [:span.fa.fa-thumbs-up
                  {:on-click (a/upvote-fn entry inc put-fn)}]
                 [:span.upvotes upvotes]
                 [:span.fa.fa-thumbs-down.toggle
                  {:on-click (a/upvote-fn entry dec put-fn)}]]))]
      [:div.word-count (md/count-words-formatted entry)]]]))

(defn entry-with-comments
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry cfg new-entries put-fn entries-map local-cfg always-show-map?]
  (let [ts (:timestamp entry)
        query-id (:query-id local-cfg)
        entry (or (get new-entries ts) entry)
        show-comments-for? (get-in cfg [:show-comments-for ts])
        comments-set (set (:comments entry))
        comments (mapv (u/find-missing-entry entries-map put-fn) comments-set)
        comments (if (:show-pvt cfg)
                   comments
                   (filter (u/pvt-filter cfg) comments))
        comments-map (into {} (map (fn [c] [(:timestamp c) c])) comments)
        toggle-comments
        #(put-fn [:cmd/assoc-in
                  {:path  [:cfg :show-comments-for ts]
                   :value (when-not (= show-comments-for? query-id) query-id)}])
        comments-filter (fn [[_ts c]] (= (:comment-for c) (:timestamp entry)))
        local-comments (into {} (filter comments-filter new-entries))
        all-comments (sort-by :timestamp (vals (merge comments-map
                                                      local-comments)))
        new-entries? (contains? new-entries ts)]
    [:div.entry-with-comments
     [journal-entry entry cfg put-fn new-entries?
      (p/pomodoro-stats-view all-comments) local-cfg always-show-map?
      (get entries-map (:linked-timestamp entry))]
     (when (seq all-comments)
       (if (= query-id show-comments-for?)
         [:div.comments
          (let [n (count comments)]
            [:div.show-comments
             (when (pos? n)
               [:span {:on-click toggle-comments}
                (str "hide " n " comment" (when (> n 1) "s"))])])
          (for [comment all-comments]
            ^{:key (str "c" (:timestamp comment))}
            [journal-entry
             comment cfg put-fn (contains? new-entries (:timestamp comment))
             nil local-cfg always-show-map? nil])]
         [:div.show-comments
          (let [n (count all-comments)]
            [:span {:on-click toggle-comments}
             (str "show " n " comment" (when (> n 1) "s"))])]))
     (when (:thumbnails cfg) [t/thumbnails entry entries-map cfg put-fn])]))
