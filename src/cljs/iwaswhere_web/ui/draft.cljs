(ns iwaswhere-web.ui.draft
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.core :as r]))

(defn editor-state-from-text
  [text]
  (let [content-from-text (.createFromText js/Draft.ContentState text)]
    (r/atom (.createWithContent js/Draft.EditorState content-from-text))))

(defn editor-state-from-raw
  [editor-state]
  (let [content-from-raw (.convertFromRaw js/Draft editor-state)]
    (r/atom (.createWithContent js/Draft.EditorState content-from-raw))))

(defn draft-search-field
  [editor-atom update-cb mentions hashtags]
  (let [editor (r/adapt-react-class
                 (aget js/window "deps" "SearchFieldEditor" "default"))
        on-change (fn [new-state]
                    (let [current-content (.getCurrentContent new-state)
                          plain (.getPlainText current-content)
                          raw-content (.convertToRaw js/Draft current-content)
                          via-json (.parse js/JSON (.stringify js/JSON raw-content))
                          editor-state (js->clj via-json :keywordize-keys true)]
                      (reset! editor-atom new-state)
                      (update-cb plain editor-state)))]
    (fn [editor-atom send-fn mentions hashtags]
      [editor {:editorState @editor-atom
               :spellCheck  true
               :mentions    mentions
               :hashtags    hashtags
               :onChange    on-change}])))
