(ns iwaswhere-web.ui.entry.story
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.entry.capture :as c]
            [iwaswhere-web.ui.draft :as d]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [clojure.string :as s]))

(defn editable-field
  [on-input-fn on-keydown-fn text]
  (fn [_ _ _]
    [:div.story-edit-field
     {:content-editable true
      :on-input         on-input-fn
      :on-key-down      on-keydown-fn}
     text]))

(defn keydown-fn
  [entry k put-fn]
  (fn [ev]
    (let [text (aget ev "target" "innerText")
          updated (assoc-in entry [k] text)
          key-code (.. ev -keyCode)
          meta-key (.. ev -metaKey)]
      (when (and meta-key (= key-code 83))                  ; CMD-s pressed
        (put-fn [:entry/update updated])
        (.preventDefault ev)))))

(defn input-fn
  [entry k put-fn]
  (fn [ev]
    (let [text (aget ev "target" "innerText")
          updated (assoc-in entry [k] text)]
      (put-fn [:entry/update-local updated]))))

(defn story-name-field
  "Renders editable field for story name when the entry is of type :story.
   Updates local entry on input, and saves the entry when CMD-S is pressed."
  [entry edit-mode? put-fn]
  (when (= (:entry-type entry) :story)
    (let [on-input-fn (input-fn entry :story-name put-fn)
          on-keydown-fn (keydown-fn entry :story-name put-fn)]
      (if edit-mode?
        [:div.story
         [:label "Story:"]
         [editable-field on-input-fn on-keydown-fn (:story-name entry)]]
        [:h2 "Story: " (:story-name entry)]))))

(defn saga-name-field
  "Renders editable field for saga name when the entry is of type :saga.
   Updates local entry on input, and saves the entry when CMD-S is pressed."
  [entry edit-mode? put-fn]
  (when (= (:entry-type entry) :saga)
    (let [on-input-fn (input-fn entry :saga-name put-fn)
          on-keydown-fn (keydown-fn entry :saga-name put-fn)]
      (if edit-mode?
        [:div.story
         [:label "Saga:"]
         [editable-field on-input-fn on-keydown-fn (:saga-name entry)]]
        [:h2 "Saga: " (:saga-name entry)]))))

(defn story-select
  "In edit mode, allow editing of story, otherwise show story name."
  [entry put-fn edit-mode?]
  (let [options (subscribe [:options])
        local (r/atom {:search ""})
        stories (reaction (:stories @options))
        story-filter (fn [[id story]]
                       (s/includes? (s/lower-case (:story-name story))
                                    (s/lower-case (:search @local))))
        sorted-stories (reaction (:sorted-stories @options))
        ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        select-handler
        (fn [ev]
          (let [selected (js/parseInt (-> ev .-nativeEvent .-target .-value))
                updated (-> (get-in @new-entries [ts])
                            (assoc-in [:linked-story] selected))]
            (put-fn [:entry/update-local updated])))
        story-input (fn [story-id]
                      (fn [_]
                        (let [updated (-> (get-in @new-entries [ts])
                                          (assoc-in [:linked-story] story-id))]
                          (swap! local assoc-in [:search] "")
                          (put-fn [:entry/update-local updated]))))
        select-story (fn [story-id]
                       (let [updated (-> (get-in @new-entries [ts])
                                         (assoc-in [:linked-story] story-id))]
                         (put-fn [:entry/update-local updated])))
        search-change (fn [ev]
                        (let [text (aget ev "target" "value")]
                          (swap! local assoc-in [:search] text)))
        story-mapper (fn [[ts story]]
                       {:name (:story-name story)
                        :id ts})]
    (fn story-select-render [entry put-fn edit-mode?]
      (let [linked-story (:linked-story entry)
            story-name (get-in @stories [linked-story :story-name] "")
            editor-state (d/editor-state-from-text story-name)
            stories-list (map story-mapper @sorted-stories)]
        (if edit-mode?
          (when-not (or (contains? #{:saga :story} (:entry-type entry))
                        (:comment-for entry))
            [:div.story
             [d/story-search-field editor-state select-story stories-list]])
          (when linked-story
            [:div.story (:story-name (get @stories linked-story))]))))))

(defn saga-select
  "In edit mode, allow editing of story, otherwise show story name."
  [entry put-fn edit-mode?]
  (let [options (subscribe [:options])
        sagas (reaction (:sagas @options))
        sorted-sagas (reaction (:sorted-sagas @options))
        ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        select-handler
        (fn [ev]
          (let [selected (js/parseInt (-> ev .-nativeEvent .-target .-value))
                updated (-> (get-in @new-entries [ts])
                            (assoc-in [:linked-saga] selected))]
            (put-fn [:entry/update-local updated])))]
    (fn story-select-render [entry put-fn edit-mode?]
      (let [linked-saga (:linked-saga entry)
            entry-type (:entry-type entry)]
        (when (= entry-type :story)
          (if edit-mode?
            (when-not (:comment-for entry)
              [:div.story
               [:label "Saga:"]
               [:select {:value     (or linked-saga "")
                         :on-change select-handler}
                [:option {:value ""} "no saga selected"]
                (for [[id saga] @sorted-sagas]
                  (let [saga-name (:saga-name saga)]
                    ^{:key (str ts saga-name)}
                    [:option {:value id} saga-name]))]])
            (when linked-saga
              [:div.story "Saga: " (:saga-name (get @sagas linked-saga))])))))))
