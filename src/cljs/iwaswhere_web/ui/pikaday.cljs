(ns iwaswhere-web.ui.pikaday
  "Wrapper around pikaday, modified from https://github.com/timgilbert/cljs-pikaday"
  (:require [reagent.core :as reagent :refer [atom]]
            [camel-snake-kebab.core :refer [->camelCaseString]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljsjs.pikaday]))

(defn- opts-transform [opts]
  "Given a clojure map of options, return a js object for a pikaday constructor argument."
  (clj->js (transform-keys ->camelCaseString opts)))

(defn date-selector
  "Return a date-selector reagent component. Takes a single map as its
  argument, with the following keys:
  date-atom: an atom or reaction bound to the date value represented by the picker.
  max-date-atom: atom representing the maximum date for the selector.
  min-date-atom: atom representing the minimum date for the selector.
  pikaday-attrs: a map of options to be passed to the Pikaday constructor.
  input-attrs: a map of options to be used as <input> tag attributes."
  [{:keys [date callback pikaday-attrs]}]
  (let [instance-atom (atom nil)
        did-mount (fn [this]
                    (let [default-opts
                          {:field            (js/ReactDOM.findDOMNode this)
                           :default-date     date
                           :set-default-date true
                           :on-select        callback}
                          opts (opts-transform (merge default-opts pikaday-attrs))
                          instance (js/Pikaday. opts)]
                      (reset! instance-atom instance)))
        unmount (fn [this]
                  (.destroy @instance-atom)
                  (reset! instance-atom nil))
        render (fn [props] [:input (:input-attrs props)])]
    (reagent/create-class
      {:component-did-mount    did-mount
       :component-will-unmount unmount
       :display-name           "pikaday-component"
       :reagent-render         render})))