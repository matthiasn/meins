(ns iwaswhere-web.ui.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.ui.pomodoro :as p]
            [cljsjs.moment]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [reagent.core :as r]
            [cljs.pprint :as pp]))

(defn hashtags-mentions-list
  "Horizontally renders list with hashtags and mentions."
  [entry]
  (let [tags (:tags entry)
        mentions (:mentions entry)]
    [:div.hashtags
     (for [mention mentions]
       ^{:key (str "tag-" mention)}
       [:span.mention mention])
     (for [hashtag tags]
       ^{:key (str "tag-" hashtag)}
       [:span.hashtag hashtag])]))

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
        [:span.fa.fa-trash-o.toggle {:on-click guarded-trash-fn}]))))

(defn new-link
  "Renders input for adding link entry."
  [entry put-fn create-linked-entry]
  (let [visible (r/atom false)]
    (fn [entry put-fn create-linked-entry]
      [:span.new-link-btn
       [:span.fa.fa-link.toggle {:on-click #(swap! visible not)}]
       (when @visible
         [:span.new-link
          [:span.fa.fa-plus-square {:on-click #(do (create-linked-entry) (swap! visible not))}]
          [:input {:on-click    #(.stopPropagation %)
                   :on-key-down (fn [ev]
                                  (when (= (.-keyCode ev) 13)
                                    (let [link (re-find #"[0-9]{13}" (.-value (.-target ev)))
                                          entry-links (:linked-entries entry)
                                          linked-entries (conj entry-links (long link))
                                          new-entry (h/clean-entry
                                                      (merge entry
                                                             {:linked-entries linked-entries}))]
                                      (when link
                                        (put-fn [:entry/update new-entry])
                                        (swap! visible not)))))}]])])))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry cfg put-fn edit-mode? info]
  (let [ts (:timestamp entry)
        map? (:latitude entry)
        show-map? (contains? (:show-maps-for cfg) ts)
        toggle-map #(put-fn [:cmd/toggle {:timestamp ts :path [:cfg :show-maps-for]}])
        show-comments? (contains? (:show-comments-for cfg) ts)
        toggle-comments #(put-fn [:cmd/toggle {:timestamp ts :path [:cfg :show-comments-for]}])
        create-comment (h/new-entry-fn put-fn {:comment-for ts})
        create-linked-entry (h/new-entry-fn put-fn {:linked-entries [ts]})
        create-pomodoro #(do ((h/new-entry-fn put-fn (p/pomodoro-defaults ts)))
                             (put-fn [:cmd/set-opt {:timestamp ts
                                                    :path      [:cfg :show-comments-for]}]))
        toggle-edit #(if edit-mode? (put-fn [:entry/remove-local entry])
                                    (put-fn [:entry/update-local entry]))
        trash-entry #(if edit-mode? (put-fn [:entry/remove-local {:timestamp ts}])
                                    (put-fn [:entry/trash {:timestamp ts}]))
        upvotes (:upvotes entry)
        upvote-fn (fn [op] #(put-fn [:entry/update (update-in entry [:upvotes] op)]))
        hashtags (:hashtags cfg)
        mentions (:mentions cfg)
        arrival-ts (:arrival-timestamp entry)
        depart-ts (:departure-timestamp entry)
        dur (when (and arrival-ts depart-ts) (-> (- depart-ts arrival-ts)
                                                 (/ 1000)
                                                 (Math/floor)))
        formatted-duration (when (and dur (< dur 99999)) (str ", " (u/duration-string dur)))]
    [:div.entry
     [:div.header
      [:div
       [:a {:href (str "/#" (.format (js/moment ts) "YYYY-MM-DD"))}
        [:time (.format (js/moment ts) "ddd, MMMM Do YYYY")]]
       [:time (.format (js/moment ts) ", h:mm a") formatted-duration]]
      (if (= :pomodoro (:entry-type entry))
        [p/pomodoro-header entry #(put-fn [:cmd/pomodoro-start entry]) edit-mode?]
        [:div info])
      [:div
       (when (seq (:linked-entries-list entry))
         (let [entry-active? (= (:active cfg) (:timestamp entry))
               set-active-fn #(put-fn [:cmd/toggle-active (:timestamp entry)])]
           [:span.link-btn {:on-click set-active-fn :class (when entry-active? "active")}
            (str " linked: " (count (:linked-entries-list entry)))]))]
      [:div
       [:span.fa.toggle
        {:on-click (upvote-fn inc) :class (if (pos? upvotes) "fa-thumbs-up" "fa-thumbs-o-up")}]
       (when (pos? upvotes) [:span.upvotes " " upvotes])
       (when (pos? upvotes) [:span.fa.fa-thumbs-down.toggle {:on-click (upvote-fn dec)}])
       (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
       [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
       (when-not (:comment-for entry) [:span.fa.fa-clock-o.toggle {:on-click create-pomodoro}])
       (when-not (:comment-for entry) [:span.fa.fa-comment-o.toggle {:on-click create-comment}])
       (when (seq (:comments entry))
         [:span.fa.fa-comments.toggle {:on-click toggle-comments
                                       :class    (when-not show-comments? "hidden-comments")}])
       (when-not (:comment-for entry)
         [:a {:href (str "/#" ts) :target "_blank"} [:span.fa.fa-external-link.toggle]])
       (when-not (:comment-for entry) [new-link entry put-fn create-linked-entry])
       [trash-icon trash-entry]]]
     [hashtags-mentions-list entry]
     [l/leaflet-map entry (or show-map? (:show-all-maps cfg))]
     (if edit-mode? [e/editable-md-render entry hashtags mentions put-fn toggle-edit]
                    [md/markdown-render entry (:show-hashtags cfg)])
     [m/image-view entry]
     [m/audioplayer-view entry]
     [m/videoplayer-view entry]]))

(defn entry-with-comments
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry cfg new-entries put-fn]
  (let [ts (:timestamp entry)
        entry (or (get new-entries ts) entry)
        comments (:comments entry)
        comments (if (:show-pvt cfg) comments (filter u/pvt-filter comments))
        comments-map (into {} (map (fn [c] [(:timestamp c) c])) comments)
        toggle-comments #(put-fn [:cmd/toggle {:timestamp ts :path [:cfg :show-comments-for]}])
        local-comments (into {} (filter (fn [[_ts c]] (= (:comment-for c) (:timestamp entry)))
                                        new-entries))
        all-comments (sort-by :timestamp (vals (merge comments-map local-comments)))
        new-entries? (contains? new-entries ts)]
    [:div.entry-with-comments
     [journal-entry entry cfg put-fn new-entries? (p/pomodoro-stats-view all-comments)]
     (when (seq all-comments)
       (if (or (contains? (:show-comments-for cfg) ts) (seq local-comments))
         [:div.comments
          (for [comment all-comments]
            ^{:key (str "c" (:timestamp comment))}
            [journal-entry comment cfg put-fn (contains? new-entries (:timestamp comment))])]
         [:div.show-comments {:on-click toggle-comments}
          (let [n (count comments)]
            [:span (str "show " n " comment" (when (> n 1) "s"))])]))]))
