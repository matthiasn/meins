(ns iwaswhere-web.ui.entry
  (:require [iwaswhere-web.ui.leaflet :as l]
            [iwaswhere-web.ui.markdown :as md]
            [iwaswhere-web.ui.edit :as e]
            [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.ui.pomodoro :as p]
            [iwaswhere-web.utils.parse :as up]
            [cljsjs.moment]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [reagent.core :as r]
            [cljs.pprint :as pp]
            [clojure.set :as set]))

(defn hashtags-mentions-list
  "Horizontally renders list with hashtags and mentions."
  [entry cfg]
  [:div.hashtags
   (when (:redacted cfg) {:class "redacted"})
   (for [mention (:mentions entry)]
     ^{:key (str "tag-" mention)}
     [:span.mention mention])
   (for [hashtag (:tags entry)]
     ^{:key (str "tag-" hashtag)}
     [:span.hashtag hashtag])])

(defn trash-icon
  "Renders a trash icon, which transforms into a warning button that needs to be
   clicked again for actual deletion. This label is a little to the right, so it
   can't be clicked accidentally, and disappears again within 5 seconds."
  [trash-fn]
  (let [clicked (r/atom false)
        guarded-trash-fn (fn [_ev]
                           (swap! clicked not)
                           (.setTimeout js/window #(reset! clicked false) 5000))]
    (fn [trash-fn]
      (if @clicked
        [:span.delete-warn {:on-click trash-fn}
         [:span.fa.fa-trash] "  confirm delete?"]
        [:span.fa.fa-trash-o.toggle {:on-click guarded-trash-fn}]))))

(defn edit-icon
  "Renders an edit icon, which transforms into a warning button that needs to be
   clicked again for actually discarding changes. This label is a little to the
   right, so it can't be clicked accidentally, and disappears again within 5
   seconds."
  [toggle-edit edit-mode?]
  (let [clicked (r/atom false)
        guarded-edit-fn (fn [_ev]
                          (swap! clicked not)
                          (.setTimeout js/window #(reset! clicked false) 5000))]
    (fn [toggle-edit edit-mode?]
      (if edit-mode?
        (if @clicked
          [:span.delete-warn {:on-click #(do (toggle-edit) (swap! clicked not))}
           [:span.fa.fa-trash] "  discard changes?"]
          [:span.fa.fa-pencil-square-o.toggle {:on-click guarded-edit-fn}])
        [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]))))

(defn new-link
  "Renders input for adding link entry."
  [entry put-fn cfg create-linked-entry]
  (let [local (r/atom {:visible     false
                       :last-linked nil})
        toggle-visible #(swap! local update-in [:visible] not)
        keydown-fn
        (fn [ev]
          (when (= (.-keyCode ev) 13)
            (let [link (re-find #"[0-9]{13}" (.-value (.-target ev)))
                  linked-entries (conj (:linked-entries entry) (long link))
                  new-entry (h/clean-entry
                              (merge entry {:linked-entries linked-entries}))]
              (when link
                (put-fn [:entry/update new-entry])
                (swap! local update-in [:visible] not)))))

        on-drag-start (fn [ev]
                        (let [dt (.-dataTransfer ev)]
                          (put-fn [:cmd/set-dragged entry])
                          (aset dt "effectAllowed" "move")
                          (aset dt "dropEffect" "link")))]
    (fn [entry put-fn cfg create-linked-entry]
      (let [on-drag-over
            (fn [ev]
              (let [ts (:currently-dragged cfg)
                    new-entry (update-in entry [:linked-entries] #(set (conj % ts)))]
                (when (and (not= ts (:last-linked @local))
                           (not= ts (:timestamp entry)))
                  (put-fn [:entry/update (h/clean-entry new-entry)]))
                (swap! local assoc-in [:last-linked] ts)))]
        [:span.new-link-btn
         [:span.fa.fa-link.toggle {:on-click      toggle-visible
                                   :draggable     true
                                   :on-drag-start on-drag-start
                                   :on-drag-over  on-drag-over}]
         (when (:visible @local)
           [:span.new-link
            [:span.fa.fa-plus-square
             {:on-click #(do (create-linked-entry) (toggle-visible))}]
            [:input {:on-click    #(.stopPropagation %)
                     :on-key-down keydown-fn}]])]))))

(defn entry-actions
  "Entry-related action buttons. Hidden by default, become visible when mouse
   hovers over element, stays visible for a little while after mose leaves."
  [entry cfg put-fn edit-mode? toggle-edit local-cfg]
  (let [visible (r/atom false)
        hide-fn (fn [_ev]
                  (.setTimeout js/window #(reset! visible false) 60000))]
    (fn
      [entry cfg put-fn edit-mode? toggle-edit local-cfg]
      (let [ts (:timestamp entry)
            query-id (:query-id local-cfg)
            tab-group (:tab-group local-cfg)
            map? (:latitude entry)
            toggle-map #(put-fn [:cmd/toggle
                                 {:timestamp ts
                                  :path      [:cfg :show-maps-for]}])
            show-comments? (= query-id (get-in cfg [:show-comments-for ts]))
            show-hide-comments #(put-fn [:cmd/assoc-in
                                         {:path  [:cfg :show-comments-for ts]
                                          :value %}])
            show-comments #(show-hide-comments query-id)
            toggle-comments #(show-hide-comments
                              (when-not show-comments? query-id))
            create-comment (h/new-entry-fn put-fn {:comment-for ts} show-comments)
            create-linked-entry (h/new-entry-fn put-fn {:linked-entries [ts]} nil)
            new-pomodoro (h/new-entry-fn
                           put-fn (p/pomodoro-defaults ts) show-comments)
            add-activity #(put-fn [:entry/update-local
                                   (assoc-in entry [:activity]
                                             {:name ""
                                              :duration-m 0
                                              :exertion-level 5})])
            add-consumption
            (fn [_ev]
              (put-fn [:entry/update-local
                       (-> entry
                           (assoc-in [:consumption]
                                     {:name     ""
                                      :quantity 0})
                           (update-in [:tags] conj "#consumption")
                           (update-in [:md] #(str % " #consumption ")))]))
            trash-entry #(if edit-mode?
                          (put-fn [:entry/remove-local {:timestamp ts}])
                          (put-fn [:entry/trash {:timestamp ts}]))
            open-external #(put-fn [:search/add
                                    {:tab-group (if (= tab-group :right)
                                                  :left
                                                  :right)
                                     :query     (up/parse-search (str ts))}])
            upvotes (:upvotes entry)
            upvote-fn (fn [op]
                        #(put-fn [:entry/update
                                  (update-in entry [:upvotes] op)]))
            show-pvt? (:show-pvt cfg)]
        [:div {:on-mouse-enter #(reset! visible true)
               :on-drag-over   #(do (hide-fn nil) (reset! visible true))
               :on-mouse-leave hide-fn
               :style          {:opacity (if (or edit-mode? @visible) 1 0)}}
         [:span.fa.toggle
          {:on-click (upvote-fn inc)
           :class    (if (pos? upvotes) "fa-thumbs-up" "fa-thumbs-o-up")}]
         (when (pos? upvotes) [:span.upvotes upvotes])
         (when (pos? upvotes)
           [:span.fa.fa-thumbs-down.toggle {:on-click (upvote-fn dec)}])
         (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
         [edit-icon toggle-edit edit-mode?]
         (when-not (:comment-for entry)
           [:span.fa.fa-clock-o.toggle {:on-click new-pomodoro}])
         (when-not (:activity entry)
           [:span.fa.fa-bicycle.toggle {:on-click add-activity}])
         (when (and show-pvt? (not (:consumption entry)))
           [:span.fa.fa-coffee.toggle {:on-click add-consumption}])
         (when-not (:comment-for entry)
           [:span.fa.fa-comment-o.toggle {:on-click create-comment}])
         (when (seq (:comments entry))
           [:span.fa.fa-comments.toggle
            {:on-click toggle-comments
             :class    (when-not show-comments? "hidden-comments")}])
         (when-not (:comment-for entry)
           [:span.fa.fa-external-link.toggle {:on-click open-external}])
         (when-not (:comment-for entry)
           [new-link entry put-fn cfg create-linked-entry])
         [trash-icon trash-entry]]))))

(defn select-elem
  "Render select element for the given options. On change, dispatch message
   to change the local entry at the given path. When numeric? is set, coerces
   the value to int."
  [entry options path numeric? put-fn]
  (let [ts (:timestamp entry)
        select-handler (fn [ev]
                         (let [selected (-> ev .-nativeEvent .-target .-value)
                               coerced (if numeric?
                                         (js/parseInt selected)
                                         selected)]
                           (put-fn [:entry/update-local
                                    (assoc-in entry path coerced)])))]
    [:select {:value     (get-in entry path)
              :on-change select-handler}
     [:option {:value ""} ""]
     (for [opt options]
       ^{:key (str ts opt)}
       [:option {:value opt} opt])]))

(defn activity-div
  "In edit mode, allow editing of activities, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (let [activities (:activities cfg)
        ex-levels [1 2 3 4 5 6 7 8 9 10]
        durations (range 0 185 5)]
    (when-let [activity (:activity entry)]
      (if edit-mode?
        [:div
         [:label "Activity:"]
         [select-elem entry activities [:activity :name] false put-fn]
         [:label "Duration:"]
         [select-elem entry durations [:activity :duration-m] true put-fn]
         [:label "Level:"]
         [select-elem entry ex-levels [:activity :exertion-level] true put-fn]]
        [:div "Physical activity: "
         [:strong (:name activity)] " for " [:strong (:duration-m activity)]
         " min, level " [:strong (:exertion-level activity)] "/10."]))))

(defn sleep-div
  "In edit mode, allow editing of sleep data, otherwise show a summary."
  [entry put-fn edit-mode?]
  (let [quality-levels [1 2 3 4 5 6 7 8 9 10]
        interruptions [0 1 2 3 4 5 6 7 8 9 10]
        duration-m (range 0 60 5)
        duration-h (range 0 14)]
    (when (and edit-mode?
               (contains? (:tags entry) "#sleep")
               (not (:sleep entry)))
      (put-fn [:entry/update-local
               (assoc-in entry [:sleep]
                         {:duration-h    0
                          :duration-m    0
                          :quality-level 5
                          :interruptions 0})]))
    (when-let [sleep (:sleep entry)]
      (if edit-mode?
        [:div
         [:label "Sleep Hours:"]
         [select-elem entry duration-h [:sleep :duration-h] true put-fn]
         [:label "Minutes:"]
         [select-elem entry duration-m [:sleep :duration-m] true put-fn]
         [:label "Quality:"]
         [select-elem entry quality-levels [:sleep :quality-level] true put-fn]
         [:label "Interruptions:"]
         [select-elem entry interruptions [:sleep :interruptions] true put-fn]]
        [:div "Sleep: " [:strong (:duration-h sleep)] " h "
         [:strong (:duration-m sleep)] " m, quality "
         [:strong (:quality-level sleep)] "/10. "
         [:strong (:interruptions sleep)] " interruptions."]))))

(defn girth-div
  "In edit mode, allow editing of body measurement, otherwise show a summary."
  [entry put-fn edit-mode?]
  (let [girth-cm (range 85 100 1)
        girth-mm (range 0 10 1)
        girth (:girth (:measurements entry))]
    (when (and edit-mode?
               (contains? (:tags entry) "#girth")
               (not girth))
      (put-fn [:entry/update-local
               (assoc-in entry [:measurements :girth] {:abdominal-cm 0
                                                       :abdominal-mm 0})]))
    (when girth
      (if edit-mode?
        [:div
         [:label "Abdominal girth:"]
         [select-elem
          entry girth-cm [:measurements :girth :abdominal-cm] true put-fn]
         [:label "cm"]
         [select-elem
          entry girth-mm [:measurements :girth :abdominal-mm] true put-fn]
         [:label "mm"]]
        [:div "Abdominal girth: "
         [:strong (:abdominal-cm girth) "." (:abdominal-mm girth)] " cm. "]))))

(defn consumption-div
  "In edit mode, allow editing of consumption, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (let [consumption-types (:consumption-types cfg)
        quantities (range 0 10)]
    (when-let [consumption (:consumption entry)]
      (if edit-mode?
        [:div
         [:label "Consumption:"]
         [select-elem entry consumption-types [:consumption :name] false put-fn]
         [:label "Quantity:"]
         [select-elem entry quantities [:consumption :quantity] true put-fn]]
        [:div "Consumption: "
         [:strong (:name consumption)] ", quantity "
         [:strong (:quantity consumption)]]))))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry cfg put-fn edit-mode? info local-cfg]
  (let [ts (:timestamp entry)
        show-map? (contains? (:show-maps-for cfg) ts)
        toggle-edit #(if edit-mode? () ;(put-fn [:entry/remove-local entry])
                                    (put-fn [:entry/update-local entry]))
        show-pvt? (:show-pvt cfg)
        hashtags (set/union (:hashtags cfg) (:pvt-displayed cfg))
        pvt-hashtags (:pvt-hashtags cfg)
        hashtags (if show-pvt? (set/union hashtags pvt-hashtags) hashtags)
        mentions (:mentions cfg)]
    [:div.entry
     [:div.header
      [:div
       [:a {:href (str "/#" (.format (js/moment ts) "YYYY-MM-DD"))}
        ;[:time (.format (js/moment ts) "ddd, MMMM Do YYYY")]
        [:time (.format (js/moment ts) "ddd, YYYY-MM-DD HH:mm")]]
       ;[:time (.format (js/moment ts) ", h:mm a") (u/visit-duration entry)]
       [:time (u/visit-duration entry)]]
      (if (= :pomodoro (:entry-type entry))
        [p/pomodoro-header entry #(put-fn [:cmd/pomodoro-start entry]) edit-mode?]
        [:div info])
      [:div
       (when (seq (:linked-entries-list entry))
         (let [ts (:timestamp entry)
               entry-active? (contains? (set (vals (:active cfg))) ts)
               set-active-fn #(put-fn [:cmd/toggle-active
                                       {:timestamp ts
                                        :query-id (:query-id local-cfg)}])]
           [:span.link-btn {:on-click set-active-fn
                            :class    (when entry-active? "active")}
            (str " linked: " (count (:linked-entries-list entry)))]))]
      [entry-actions entry cfg put-fn edit-mode? toggle-edit local-cfg]]
     [hashtags-mentions-list entry cfg]
     [l/leaflet-map entry (or show-map? (:show-all-maps cfg)) local-cfg]
     (if edit-mode?
       [e/editable-md-render entry hashtags mentions put-fn toggle-edit]
       [md/markdown-render entry cfg toggle-edit])
     [activity-div entry cfg put-fn edit-mode?]
     [sleep-div entry put-fn edit-mode?]
     [girth-div entry put-fn edit-mode?]
     (when show-pvt?
       [consumption-div entry cfg put-fn edit-mode?])
     [m/audioplayer-view entry]
     [m/image-view entry]
     [m/videoplayer-view entry]
     (when-let [measurements (:measurements entry)]
       [:pre [:code (with-out-str (pp/pprint measurements))]])
     (when-let [upvotes (:upvotes entry)]
       (when (pos? upvotes)
         [:div
          [:span.fa.fa-thumbs-up]
          [:span.upvotes upvotes]]))]))

(defn thumbnails
  "Renders thumbnails of photos in linked entries. Respects private entries."
  [entry entries-map cfg put-fn]
  (let [ts (:timestamp entry)
        entry-active? (contains? (set (vals (:active cfg))) (:timestamp entry))
        linked-entries-set (set (:linked-entries-list entry))
        get-or-retrieve (u/find-missing-entry entries-map put-fn)
        with-imgs (filter :img-file (map get-or-retrieve linked-entries-set))
        filtered (if (:show-pvt cfg)
                   with-imgs
                   (filter (u/pvt-filter cfg) with-imgs))]
    (when-not entry-active?
      [:div.thumbnails
       (for [img-entry filtered]
         ^{:key (str "thumbnail" ts (:img-file img-entry))}
         [:div [m/image-view img-entry "?width=300"]])])))

(defn entry-with-comments
  "Renders individual journal entry. Interaction with application state happens
   via messages that are sent to the store component, for example for toggling
   the display of the edit mode or showing the map for an entry. The editable
   content component used in edit mode also sends a modified entry to the store
   component, which is useful for displaying updated hashtags, or also for
   showing the warning that the entry is not saved yet."
  [entry cfg new-entries put-fn entries-map local-cfg]
  (let [ts (:timestamp entry)
        query-id (:query-id local-cfg)
        entry (or (get new-entries ts) entry)
        comments-set (set (:comments entry))
        comments (mapv (u/find-missing-entry entries-map put-fn) comments-set)
        comments (if (:show-pvt cfg)
                   comments
                   (filter (u/pvt-filter cfg) comments))
        comments-map (into {} (map (fn [c] [(:timestamp c) c])) comments)
        toggle-comments #(put-fn [:cmd/assoc-in
                                   {:path      [:cfg :show-comments-for ts]
                                    :value query-id}])
        comments-filter (fn [[_ts c]] (= (:comment-for c) (:timestamp entry)))
        local-comments (into {} (filter comments-filter new-entries))
        all-comments (sort-by :timestamp (vals (merge comments-map
                                                      local-comments)))
        new-entries? (contains? new-entries ts)]
    [:div.entry-with-comments
     [journal-entry entry cfg put-fn new-entries?
      (p/pomodoro-stats-view all-comments) local-cfg]
     (when (seq all-comments)
       (if (= query-id (get-in cfg [:show-comments-for ts]))
         [:div.comments
          (for [comment all-comments]
            ^{:key (str "c" (:timestamp comment))}
            [journal-entry comment cfg put-fn
             (contains? new-entries (:timestamp comment)) nil local-cfg])]
         [:div.show-comments
          (let [n (count comments)]
            [:span {:on-click toggle-comments}
             (str "show " n " comment" (when (> n 1) "s"))])]))
     (when (:thumbnails cfg) [thumbnails entry entries-map cfg put-fn])]))
