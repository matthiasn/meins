(ns iwaswhere-web.ui.entry.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.ui.pomodoro :as p]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [iwaswhere-web.utils.parse :as up]
            [iwaswhere-web.ui.entry.actions :as a]
            [iwaswhere-web.ui.entry.capture :as c]
            [iwaswhere-web.ui.entry.task :as task]
            [iwaswhere-web.ui.entry.briefing :as b]
            [iwaswhere-web.ui.entry.story :as es]
            [iwaswhere-web.ui.entry.thumbnails :as t]
            [cljsjs.moment]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.helpers :as h]))

(defn hashtags-mentions-list
  "Horizontally renders list with hashtags and mentions."
  [entry tab-group put-fn]
  (let [cfg (subscribe [:cfg])
        redacted (reaction (:redacted @cfg))]
    (fn hashtags-mentions-render [entry tab-group put-fn]
      [:div.hashtags
       (when @redacted {:class "redacted"})
       (for [mention (:mentions entry)]
         ^{:key (str "tag-" mention)}
         [:span.mention {:on-click (up/add-search mention tab-group put-fn)}
          mention])
       (for [hashtag (:tags entry)]
         ^{:key (str "tag-" hashtag)}
         [:span.hashtag {:on-click (up/add-search hashtag tab-group put-fn)}
          hashtag])])))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry put-fn info local-cfg linked-desc]
  (let [cfg (subscribe [:cfg])
        ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        edit-mode (reaction (contains? @new-entries ts))
        show-map? (reaction (contains? (:show-maps-for @cfg) ts))
        active (reaction (:active @cfg))
        show-all-maps? (reaction (:show-all-maps @cfg))
        q-date-string (.format (js/moment ts) "YYYY-MM-DD")
        formatted-time (.format (js/moment ts) "ddd YY-MM-DD HH:mm")
        tab-group (:tab-group local-cfg)
        add-search (up/add-search q-date-string tab-group put-fn)
        pomo-start #(put-fn [:cmd/pomodoro-start entry])
        set-active-fn #(put-fn [:cmd/toggle-active
                                {:timestamp ts
                                 :query-id  (:query-id local-cfg)}])
        drop-fn (a/drop-linked-fn entry cfg put-fn)]
    (fn journal-entry-render [entry put-fn info local-cfg linked-desc]
      (let [edit-mode? @edit-mode
            toggle-edit #(if edit-mode? (put-fn [:entry/remove-local entry])
                                        (put-fn [:entry/update-local entry]))]
        [:div.entry {:on-drop       drop-fn
                     :on-drag-over  h/prevent-default
                     :on-drag-enter h/prevent-default}
         [es/story-select entry put-fn edit-mode?]
         [es/book-select entry put-fn edit-mode?]
         [:div.header
          [:div
           [:a [:time {:on-click add-search} formatted-time]]
           [:time (u/visit-duration entry)]]
          (if (= :pomodoro (:entry-type entry))
            [p/pomodoro-header entry pomo-start edit-mode?]
            [:div info])
          [:div
             (when (seq (:linked-entries-list entry))
               (let [ts (:timestamp entry)
                     entry-active? (when-let [query-id (:query-id local-cfg)]
                                     (= (query-id @active) ts))]
                 [:span.link-btn {:on-click set-active-fn
                                  :class    (when entry-active? "active")}
                  (str " linked: " (count (:linked-entries-list entry)))]))]
          [a/entry-actions entry put-fn edit-mode? toggle-edit local-cfg]]
         [hashtags-mentions-list entry tab-group put-fn]
         [es/story-name-field entry edit-mode? put-fn]
         [es/book-name-field entry edit-mode? put-fn]
         (if edit-mode?
           [e/editable-md-render entry put-fn]
           (if (and (empty? (:md entry)) linked-desc)
             [md/markdown-render
              (update-in linked-desc [:md]
                         #(str % " <span class=\"fa fa-link\"></span>"))
              h/prevent-default]
             [md/markdown-render entry toggle-edit]))
         [c/custom-fields-div entry put-fn edit-mode?]
         [m/audioplayer-view entry put-fn]
         [l/leaflet-map entry (or @show-map? @show-all-maps?) local-cfg put-fn]
         [m/image-view entry]
         [m/videoplayer-view entry]
         [m/imdb-view entry put-fn]
         [task/task-details entry put-fn edit-mode?]
         [task/chore-details entry put-fn edit-mode?]
         [b/briefing-view entry put-fn edit-mode?]
         [:div.footer
          [:div.likes (when-let [upvotes (:upvotes entry)]
                        (when (pos? upvotes)
                          [:div
                           [:span.fa.fa-thumbs-up
                            {:on-click (a/upvote-fn entry inc put-fn)}]
                           [:span.upvotes upvotes]
                           [:span.fa.fa-thumbs-down.toggle
                            {:on-click (a/upvote-fn entry dec put-fn)}]]))]
          [:div.word-count (u/count-words-formatted entry)]]]))))

(defn entry-with-comments
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry put-fn local-cfg]
  (let [new-entries (subscribe [:new-entries])
        ts (:timestamp entry)
        comments-filter (fn [[_ts c]] (= (:comment-for c) ts))
        local-comments (reaction (into {} (filter comments-filter @new-entries)))
        entries-map (subscribe [:entries-map])
        new-entry (reaction (get @new-entries ts))
        cfg (subscribe [:cfg])
        options (subscribe [:options])
        show-pvt? (reaction (:show-pvt @cfg))
        thumbnails? (reaction (:thumbnails @cfg))
        show-comments-for? (reaction (get-in @cfg [:show-comments-for ts]))
        query-id (:query-id local-cfg)
        toggle-comments
        #(put-fn [:cmd/assoc-in
                  {:path  [:cfg :show-comments-for ts]
                   :value (when-not (= @show-comments-for? query-id) query-id)}])]
    (fn entry-with-comments-render [entry put-fn local-cfg]
      (let [entry (or @new-entry entry)
            comments-set (set (:comments entry))
            comments (mapv (u/find-missing-entry @entries-map put-fn) comments-set)
            comments (if @show-pvt?
                       comments
                       (filter (u/pvt-filter @options) comments))
            comments-map (into {} (map (fn [c] [(:timestamp c) c])) comments)
            all-comments (sort-by :timestamp (vals (merge comments-map
                                                          @local-comments)))]
        [:div.entry-with-comments
         [journal-entry entry put-fn
          (p/pomodoro-stats-view all-comments) local-cfg
          (get @entries-map (:linked-timestamp entry))]
         (when (seq all-comments)
           (if (= query-id @show-comments-for?)
             [:div.comments
              (let [n (count comments)]
                [:div.show-comments
                 (when (pos? n)
                   [:span {:on-click toggle-comments}
                    (str "hide " n " comment" (when (> n 1) "s"))])])
              (for [comment all-comments]
                ^{:key (str "c" (:timestamp comment))}
                [journal-entry comment put-fn nil local-cfg nil])]
             [:div.show-comments
              (let [n (count all-comments)]
                [:span {:on-click toggle-comments}
                 (str "show " n " comment" (when (> n 1) "s"))])]))
         (when @thumbnails? [t/thumbnails entry put-fn])]))))
