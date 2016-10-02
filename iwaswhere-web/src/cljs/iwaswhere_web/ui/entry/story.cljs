(ns iwaswhere-web.ui.entry.story
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.entry.capture :as c]))

(defn editable-field
  [on-input-fn on-keydown-fn text]
  [:div.story-edit-field
   {:content-editable true
    :on-input         on-input-fn
    :on-key-down      on-keydown-fn}
   text])

(defn story-name
  "Renders editable field for story name when the entry is of type :story.
   Updates local entry on input, and saves the entry when CMD-S is pressed."
  [entry put-fn]
  (when (= (:entry-type entry) :story)
    (let [on-input-fn (fn [ev]
                        (let [text (aget ev "target" "innerText")
                              updated (assoc-in entry [:story-name] text)]
                          (put-fn [:entry/update-local updated])))
          on-keydown-fn (fn [ev]
                          (let [text (aget ev "target" "innerText")
                                updated (assoc-in entry [:story-name] text)
                                key-code (.. ev -keyCode)
                                meta-key (.. ev -metaKey)]
                            (when (and meta-key (= key-code 83)) ; CMD-s pressed
                              (put-fn [:entry/update updated])
                              (.preventDefault ev))))]
      [:div.story
       [:label "Story:"]
       [editable-field on-input-fn on-keydown-fn (:story-name entry)]])))

(defn story-select
  "In edit mode, allow editing of activities, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (let [ts (:timestamp entry)
        select-handler
        (fn [ev]
          (let [selected (js/parseInt (-> ev .-nativeEvent .-target .-value))]
            (put-fn [:entry/update-local
                     (assoc-in entry [:linked-story] selected)])))]
    (when edit-mode?
      (when-not (or (= (:entry-type entry) :story) (:comment-for entry))
        [:div.story
         [:label "Story:"]
         [:select {:value     (:linked-story entry)
                   :on-change select-handler}
          [:option {:value ""} "no story selected"]
          (for [[id story] (:stories cfg)]
            (let [story-name (:story-name story)]
              ^{:key (str ts story-name)}
              [:option {:value id} story-name]))]]))))
