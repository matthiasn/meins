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
        [u/span-w-tooltip "fa-trash-o" "delete entry" guarded-trash-fn]))))

(defn new-link
  [entry put-fn]
  (let [visible (r/atom false)]
    (fn [entry put-fn]
      [:span.fa.fa-link.toggle.new-link-btn {:on-click #(swap! visible not)}
       (when @visible
         [:span.new-link-entry
          [:input {:on-click    #(.stopPropagation %)
                   :on-key-down (fn [ev]
                                  (when (= (.-keyCode ev) 13)
                                    (let [link (re-find #"[0-9]{13}" (.-value (.-target ev)))
                                          entry-links (:linked-entries entry)
                                          linked-entries (conj entry-links (long link))
                                          #_#_
                                          new-entry (-> (merge entry {:linked-entries linked-entries})
                                                        (dissoc :comments)
                                                        (dissoc :linked-entries-list))
                                          new-entry (h/clean-entry (merge entry {:linked-entries linked-entries}))]
                                      (when link
                                        (put-fn [:text-entry/update new-entry])
                                        (swap! visible not)))))}]])])))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry? with-linked?]
  (let [local (rc/atom {:edit-mode (contains? (:tags entry) "#new-entry")})]
    (fn
      [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry? with-linked?]
      (let [ts (:timestamp entry)
            map? (:latitude entry)
            show-map? (contains? (:show-maps-for store-snapshot) ts)
            toggle-map #(put-fn [:cmd/toggle {:timestamp ts :key :show-maps-for}])
            toggle-edit #(swap! local update-in [:edit-mode] not)
            trash-entry #(if new-entry?
                          (put-fn [:entry/remove-local {:timestamp ts}])
                          (put-fn [:cmd/trash {:timestamp ts}]))
            upvotes (:upvotes entry)
            upvote-fn (fn [op] #(put-fn [:text-entry/update (update-in entry [:upvotes] op)]))

            arrival-ts (:arrival-timestamp entry)
            departure-ts (:departure-timestamp entry)
            dur (when (and arrival-ts departure-ts)
                  (-> (- departure-ts arrival-ts) (/ 60000) (Math/floor)))
            formatted-duration (when (< dur 99999)
                                 (let [minutes (rem dur 60)
                                       hours (Math/floor (/ dur 60))]
                                   (str ", " (when (pos? hours) (str hours "h "))
                                        (when (pos? minutes) (str minutes "m")))))]
        [:div.entry
         [:div.entry-header
          [:a {:href (str "/#" (.format (js/moment ts) "YYYY-MM-DD"))}
           [:span.timestamp (.format (js/moment ts) "dddd, MMMM Do YYYY")]]
          [:span.timestamp (.format (js/moment ts) ", h:mm a") formatted-duration]
          [:span.fa.toggle.tooltip {:class (if (pos? upvotes) "fa-thumbs-up" "fa-thumbs-o-up") :on-click (upvote-fn inc)}
           [:span.tooltiptext "upvote"]]
          (when (pos? upvotes) [:span.upvotes " " upvotes])
          (when (pos? upvotes) [u/span-w-tooltip "fa-thumbs-down" "dislike" (upvote-fn dec)])
          (when map? [u/span-w-tooltip "fa-map-o" "show map" toggle-map])
          [u/span-w-tooltip "fa-pencil-square-o" "edit entry" toggle-edit]
          (when-not (:comment-for entry)
            [u/span-w-tooltip "fa-comment-o" "new comment" (h/new-entry-fn put-fn {:comment-for ts})])
          (when (seq (:comments entry))
            [u/span-w-tooltip (str "fa-comments " (when-not @show-comments? "hidden-comments"))
             "show comments" #(swap! show-comments? not)])
          (when-not (:comment-for entry)
            [:a.tooltip {:href  (str "/#" ts) :target "_blank"} [:span.fa.fa-external-link.toggle]
             [:span.tooltiptext "open in external tab"]])
          (when with-linked? [new-link entry put-fn])
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
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-all-comments?
   new-entry? with-linked?]
  (let [show-comments? (r/atom true)]
    (fn [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-all-comments?
         new-entry? with-linked?]
      [:div.entry-with-comments
       [journal-entry
        entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? new-entry? with-linked?]
       (when (and @show-comments? show-all-comments?)
         [:div.comments
          (let [comments (:comments entry)]
            (for [comment (if show-pvt? comments (filter u/pvt-filter comments))]
              ^{:key (str "c" (:timestamp comment))}
              [journal-entry
               comment store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? false false]))
          (for [comment (filter #(= (:comment-for %) (:timestamp entry)) (vals (:new-entries store-snapshot)))]
            ^{:key (str "c" (:timestamp comment))}
            [journal-entry
             comment store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-comments? true false])])])))

(defn entry-w-linked-entries
  [entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-all-comments? new-entry?]
  [:div.entry-with-linked
   [entry-with-comments entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt?
    show-all-comments? new-entry? true]
   (when-let [linked-entries (:linked-entries-list entry)]
     (when (seq linked-entries)
       [:div.linked-entries
        [:h6 "Linked entries"]
        (for [linked-entry linked-entries]
          ^{:key (str "c" (:timestamp linked-entry))}
          [entry-with-comments linked-entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags?
           show-pvt? show-all-comments? new-entry? false])]))])
