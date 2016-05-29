(ns iwaswhere-web.ui.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.ui.pomodoro :as p]
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
  [entry put-fn]
  (let [visible (r/atom false)]
    (fn [entry put-fn]
      [:span.fa.fa-link.toggle.new-link-btn {:on-click #(swap! visible not)}
       (when @visible
         [:input {:on-click    #(.stopPropagation %)
                  :on-key-down (fn [ev]
                                 (when (= (.-keyCode ev) 13)
                                   (let [link (re-find #"[0-9]{13}" (.-value (.-target ev)))
                                         entry-links (:linked-entries entry)
                                         linked-entries (conj entry-links (long link))
                                         new-entry (h/clean-entry (merge entry {:linked-entries linked-entries}))]
                                     (when link
                                       (put-fn [:text-entry/update new-entry])
                                       (swap! visible not)))))}])])))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry store-snapshot put-fn show-comments?]
  (let [local (rc/atom {:edit-mode (:new-entry entry)})]
    (fn
      [entry store-snapshot put-fn show-comments?]
      (let [ts (:timestamp entry)
            map? (:latitude entry)
            new-entry? (:new-entry entry)
            show-map? (contains? (:show-maps-for store-snapshot) ts)
            toggle-map #(put-fn [:cmd/toggle {:timestamp ts :path [:cfg :show-maps-for]}])
            toggle-edit #(do (if (:edit-mode @local)
                               (put-fn [:entry/remove-local entry])
                               (put-fn [:entry/update-local (merge {:new-entry true} entry)]))
                             (swap! local update-in [:edit-mode] not))
            trash-entry #(if new-entry?
                          (put-fn [:entry/remove-local {:timestamp ts}])
                          (put-fn [:cmd/trash {:timestamp ts}]))
            upvotes (:upvotes entry)
            upvote-fn (fn [op] #(put-fn [:text-entry/update (update-in entry [:upvotes] op)]))
            hashtags (:hashtags store-snapshot)
            mentions (:mentions store-snapshot)
            arrival-ts (:arrival-timestamp entry)
            departure-ts (:departure-timestamp entry)
            dur (when (and arrival-ts departure-ts)
                  (-> (- departure-ts arrival-ts) (/ 60000) (Math/floor)))
            formatted-duration (when (and dur (< dur 99999))
                                 (let [minutes (rem dur 60)
                                       hours (Math/floor (/ dur 60))]
                                   (str ", " (when (pos? hours) (str hours "h "))
                                        (when (pos? minutes) (str minutes "m")))))]
        [:div.entry
         [:div.header
          [:div
           [:a {:href (str "/#" (.format (js/moment ts) "YYYY-MM-DD"))}
            [:time (.format (js/moment ts) "ddd, MMMM Do YYYY")]]
           [:time (.format (js/moment ts) ", h:mm a") formatted-duration]]
          [:div
           (when (seq (:linked-entries-list entry))
             (let [entry-active? (= (-> store-snapshot :active) (:timestamp entry))]
               [:span.link-btn {:on-click #(put-fn [:cmd/set-active (if entry-active? nil (:timestamp entry))])
                                :class    (when entry-active? "active")}
                (str " linked: " (count (:linked-entries-list entry)))]))]
          [:div
           [:span.fa.toggle {:on-click (upvote-fn inc) :class (if (pos? upvotes) "fa-thumbs-up" "fa-thumbs-o-up")}]
           (when (pos? upvotes) [:span.upvotes " " upvotes])
           (when (pos? upvotes) [:span.fa.fa-thumbs-down.toggle {:on-click (upvote-fn dec)}])
           (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
           [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
           (when-not (:comment-for entry)
             [:span.fa.fa-clock-o.toggle {:on-click (h/new-entry-fn put-fn (p/pomodoro-defaults ts))}])
           (when-not (:comment-for entry)
             [:span.fa.fa-comment-o.toggle {:on-click #(do ((h/new-entry-fn put-fn {:comment-for ts}))
                                                           (reset! show-comments? true))}])
           (when (seq (:comments entry))
             [:span.fa.fa-comments.toggle {:on-click #(swap! show-comments? not)
                                           :class    (when-not @show-comments? "hidden-comments")}])
           (when-not (:comment-for entry)
             [:a {:href (str "/#" ts) :target "_blank"} [:span.fa.fa-external-link.toggle]])
           (when-not (:comment-for entry) [new-link entry put-fn])
           [trash-icon trash-entry]]]
         (when (= :pomodoro (:entry-type entry)) [p/pomodoro-header entry put-fn])
         [hashtags-mentions-list entry]
         [l/leaflet-map entry (or show-map? (:show-all-maps store-snapshot))]
         (if (or new-entry? (:edit-mode @local))
           [e/editable-md-render entry hashtags mentions put-fn toggle-edit]
           [md/markdown-render entry (:show-hashtags store-snapshot)])
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
  [entry cfg new-entries put-fn]
  (let [show-comments? (r/atom false)]
    (fn [entry cfg new-entries put-fn]
      (let [comments (:comments entry)
            comments (if (:show-pvt cfg) comments (filter u/pvt-filter comments))
            comments-map (into {} (map (fn [c] [(:timestamp c) c])) comments)
            local-comments (into {} (filter (fn [[_ts c]] (= (:comment-for c) (:timestamp entry))) new-entries))
            all-comments (sort-by :timestamp (vals (merge comments-map local-comments)))]
        [:div.entry-with-comments
         [journal-entry entry cfg put-fn show-comments?]
         (when (seq all-comments)
           (if (or @show-comments? (seq local-comments))
             [:div.comments
              (for [comment all-comments]
                ^{:key (str "c" (:timestamp comment))}
                [journal-entry comment cfg put-fn show-comments?])]
             [:div.show-comments {:on-click #(swap! show-comments? not)}
              (let [n (count comments)]
                [:span (str "show " n " comment" (when (> n 1) "s"))])]))]))))
