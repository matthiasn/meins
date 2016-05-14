(ns iwaswhere-web.ui.markdown
  (:require [markdown.core :as md]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [matthiasn.systems-toolbox-ui.helpers :as uh]
            [reagent.core :as r]
            [clojure.string :as s]
            [cljsjs.moment]))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous here since
  application is only running locally, so in doubt we could only harm ourselves.
  Returns nil when entry does not contain markdown text."
  [entry show-hashtags?]
  (when-let [md-string (:md entry)]
    (let [formatted-md (-> md-string
                           (u/reducer (:tags entry) (u/hashtags-replacer show-hashtags?))
                           (u/reducer (:mentions entry) u/mentions-replacer))]
      [:div {:dangerouslySetInnerHTML {:__html (md/md->html formatted-md)}}])))

(defn suggestions
  "Renders suggestions for hashtags or mentions if either occurs before the current caret position.
  It does so by getting the selection from the DOM API, which can be used to determine the position
  and a string before that position, then finding either a hashtag or mention fragment right at the
  and of that substring. For these, auto-suggestions are displayed, which are entities that begin
  with the tag fragment before the caret position. When any of the suggestions are clicked, the
  fragment will be replaced with the clicked item."
  [entry hashtags mentions local-saved-entry local-display-entry]
  (let [ts (:ts entry)
        selection (.getSelection js/window)
        cursor-pos (.-anchorOffset selection)
        anchor-node (aget selection "anchorNode")
        node-value (str (when anchor-node (aget anchor-node "nodeValue")) "")
        md (:md @local-saved-entry)
        before-cursor (subs node-value 0 cursor-pos)
        current-tag (re-find (js/RegExp. (str "(?!^)#" h/tag-char-class "+$") "m") before-cursor)
        current-tag-regex (js/RegExp. current-tag "i")
        tag-substr-filter (fn [tag] (when current-tag (re-find current-tag-regex tag)))
        current-mention (re-find (js/RegExp. (str "@" h/tag-char-class  "+$") "m") before-cursor)
        current-mention-regex (js/RegExp. current-mention "i")
        mention-substr-filter (fn [mention] (when current-mention (re-find current-mention-regex mention)))
        tags-fn (fn [tag-substr-filter curr-tag-regex tags]
                  (for [tag (filter tag-substr-filter tags)]
                    ^{:key (str ts tag)}
                    [:div
                     {:on-click #(let [updated (merge entry (h/parse-entry (s/replace md curr-tag-regex tag)))]
                                  (reset! local-saved-entry updated)
                                  (reset! local-display-entry updated))}
                     [:span.hashtag tag]]))]
    [:div.suggestions
     (tags-fn tag-substr-filter (js/RegExp (str current-tag "(?!" h/tag-char-class ")") "i") hashtags)
     (tags-fn mention-substr-filter current-mention mentions)]))

(defn editable-md-render
  "Renders markdown in a pre>code element, with editable content. Sends update message to store
  component on any change to the component. The save button sends updated entry to the backend."
  [entry hashtags mentions put-fn toggle-edit]
  (let [entry (dissoc entry :comments)
        last-saved (r/atom entry)
        local-saved-entry (r/atom entry)
        local-display-entry (r/atom entry)]
    (fn [entry hashtags mentions put-fn toggle-edit]
      (let [latest-entry (dissoc entry :comments)
            md-string (or (:md @local-display-entry) "edit here")
            get-content #(aget (.. % -target -parentElement -parentElement -firstChild -firstChild) "innerText")
            update-temp-fn #(reset! local-saved-entry (merge latest-entry (h/parse-entry (get-content %))))
            save-fn #(put-fn [:text-entry/update @local-saved-entry])
            on-keydown-fn (fn [ev] (let [key-code (.. ev -keyCode)
                                         meta-key (.. ev -metaKey)]
                                     (when (and meta-key (= key-code 83))
                                       (if (not= @last-saved @local-saved-entry) ; when no change, toggle edit mode
                                         (save-fn)
                                         (toggle-edit))
                                       (.preventDefault ev))
                                     (when (= key-code 9)
                                       ;(put-fn [:text-entry/update temp-entry])
                                       (prn key-code)
                                       (.preventDefault ev))))]
        (when-not (= @last-saved latest-entry) (reset! last-saved latest-entry)
                                               (reset! local-display-entry latest-entry)
                                               (reset! local-saved-entry latest-entry))
        [:div.edit-md
         [:pre [u/initial-focus-wrapper
                [:code {:content-editable true
                        :on-input         update-temp-fn
                        :on-key-down      on-keydown-fn}
                 md-string]]]
         [suggestions entry hashtags mentions local-saved-entry local-display-entry]
         (when (not= @last-saved @local-saved-entry)
           [:span.not-saved {:on-click save-fn} [:span.fa.fa-floppy-o] "  click to save"])]))))

(defn md-render
  "Helper for conditionally either showing rendered output or editable markdown."
  [entry hashtags mentions put-fn editable? toggle-edit show-hashtags?]
  (if editable?
    [editable-md-render entry hashtags mentions put-fn toggle-edit]
    [markdown-render entry show-hashtags?]))
