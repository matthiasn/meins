(ns iwaswhere-web.ui.edit
  "This namespace holds the fucntions for rendering the text (markdown) content of a journal entry.
  This includes both a properly styled element for static content and the edit-mode view, with
  autosuggestions for tags and mentions."
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.utils.parse :as p]
            [reagent.core :as r]
            [clojure.string :as s]))

(defn editable-code-elem
  "Code element, with content editable. Takes md-string to render, update-temp-fn which is
   called with any input to the element, and the on-keydown-fn, which is called for each keystroke
   and can be used for intercepting key combinations such as CMD-s for saving the content.
   Also takes an atom which it will reset to the actual DOM element. This can then be used for
   focusing on the element."
  [md-string update-temp-fn on-keydown-fn edit-elem-atom]
  (r/create-class
    {:component-did-mount #(let [el (r/dom-node %)]
                            (reset! edit-elem-atom el)
                            (h/focus-on-end el))
     :reagent-render      (fn [md-string update-temp-fn on-keydown-fn edit-elem-atom]
                            [:code {:content-editable true
                                    :on-input         update-temp-fn
                                    :on-key-down      on-keydown-fn}
                             md-string])}))

(defn editable-md-render
  "Renders markdown in a pre>code element, with editable content. Sends update message to store
   component on any change to the component. The save button sends updated entry to the backend.
   Maintains some local state for storing changes before they are persisted in the backend.
   Keeps track of current cursor position and potential incomplete tags or mentions before the
   cursor, which can then be completed by clicking on an empty in the autosuggested list, or by
   using the tab key for selecting the first one."
  [entry hashtags mentions put-fn toggle-edit]
  (let [entry (-> entry (dissoc :comments) (dissoc :linked-entries))
        ts (:timestamp entry)
        edit-elem-atom (atom {})
        local-display-entry (r/atom entry)]
    (fn [entry hashtags mentions put-fn toggle-edit]
      (let [latest-entry (dissoc entry :comments)
            md-string (or (:md @local-display-entry) "edit here")
            get-content #(aget (.. % -target -parentElement -parentElement -firstChild -firstChild)
                               "innerText")
            update-temp-fn #(let [updated-entry (merge latest-entry (p/parse-entry (get-content %)))]
                             (put-fn [:entry/update-local updated-entry]))
            save-fn #(do (put-fn [:entry/update
                                  (if (and (:new-entry entry) (not (:comment-for entry)))
                                    (update-in (h/clean-entry latest-entry) [:tags] conj "#new")
                                    (h/clean-entry latest-entry))])
                         (toggle-edit))

            ; find incomplete tag or mention before cursor, show suggestions
            before-cursor (h/string-before-cursor (:md latest-entry))
            [curr-tag f-tags] (p/autocomplete-tags before-cursor "(?!^) ?#" hashtags)
            [curr-mention f-mentions] (p/autocomplete-tags before-cursor " ?@" mentions)

            replace-tag (fn [curr-tag tag]
                          (let [curr-regex (js/RegExp (str curr-tag "(?!" p/tag-char-cls ")") "i")
                                md (:md latest-entry)
                                updated (merge entry (p/parse-entry (s/replace md curr-regex tag)))]
                            (reset! local-display-entry updated)
                            (put-fn [:entry/update-local updated])
                            (.setTimeout js/window (fn [] (h/focus-on-end @edit-elem-atom)) 100)))

            on-keydown-fn (fn [ev]
                            (let [key-code (.. ev -keyCode)
                                  meta-key (.. ev -metaKey)]
                              (when (and meta-key (= key-code 83))
                                (when (not= (dissoc @local-display-entry :comments)
                                            (dissoc latest-entry :new-entry))
                                  (save-fn))
                                (.preventDefault ev))
                              (when (= key-code 9)          ; TAB key pressed
                                (when (and curr-tag (seq f-tags))
                                  (replace-tag curr-tag (first f-tags)))
                                (when (and curr-mention (seq f-mentions))
                                  (replace-tag curr-mention (first f-mentions)))
                                (.preventDefault ev))))]
        [:div.edit-md
         [:pre [editable-code-elem md-string update-temp-fn on-keydown-fn edit-elem-atom]]
         [u/suggestions ts f-tags curr-tag replace-tag "hashtag"]
         [u/suggestions ts f-mentions curr-mention replace-tag "mention"]
         (when
           (not= (dissoc @local-display-entry :comments) (dissoc latest-entry :new-entry))
           [:div
            [:span.not-saved {:on-click save-fn} [:span.fa.fa-floppy-o] "  click to save"]])]))))
