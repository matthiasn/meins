(ns iwaswhere-web.ui.charts.common)

(defn line-points
  [indexed mapper]
  (let [point-strings (map mapper indexed)]
    (apply str (interpose " " point-strings))))

(defn path
  "Renders path with the given path description attribute."
  [d]
  [:path {:stroke "rgba(200,200,200,0.5)" :stroke-width 1 :d d}])

(defn weekend?
  [date-string]
  (let [day-of-week (.weekday (js/moment. date-string))]
    (not (and (pos? day-of-week) (< day-of-week 6)))))

(defn weekend-class
  [cls v]
  (str cls (when (weekend? (:date-string v)) "-weekend")))

(defn chart-title
  [title]
  [:text {:x           300
          :y           32
          :stroke      "none"
          :fill        "#AAA"
          :text-anchor :middle
          :style       {:font-weight :bold
                        :font-size   24}}
   title])

(defn mouse-enter-fn
  "Creates event handler for mouse-enter events on elements in a chart.
   Takes a local atom and the value associated with the chart element.
   Returns function which detects the mouse position from the event and
   replaces :mouse-over key in local atom with v and :mouse-pos with the
   mouse position in the event"
  [local v]
  (fn [ev]
    (let [mouse-pos {:x (.-pageX ev) :y (.-pageY ev)}
          update-fn (fn [state v]
                      (-> state
                          (assoc-in [:mouse-over] v)
                          (assoc-in [:mouse-pos] mouse-pos)))]
      (swap! local update-fn v))))

(defn mouse-leave-fn
  [local v]
  (fn [_ev]
    (when (= v (:mouse-over @local)) (reset! local {}))))