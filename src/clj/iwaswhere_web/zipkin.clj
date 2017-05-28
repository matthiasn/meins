(ns iwaswhere-web.zipkin
  (:import brave.Tracing)
  (:import brave.Tracer)
  (:import zipkin.reporter.okhttp3.OkHttpSender)
  (:import zipkin.reporter.AsyncReporter))

(def sender (OkHttpSender/create "http://127.0.0.1:9411/api/v1/spans"))
(def reporter (AsyncReporter/create sender))

(def tracing (-> (Tracing/newBuilder)
                 (.localServiceName "store")
                 (.reporter reporter)
                 (.build)))

(def prop-setter
  (reify brave.propagation.Propagation$Setter
    (put [this c k v]
      (assoc! c k v))))


(def prop-getter
  (reify brave.propagation.Propagation$Getter
    (get [this c k]
      (get-in c [k]))))

;tracing.close();
;reporter.close();


(defn trace-wrapper [f service-name]
  (let [tracing (-> (Tracing/newBuilder)
                    (.localServiceName service-name)
                    (.reporter reporter)
                    (.build))
        tracer (.tracer tracing)
        injector (-> tracing
                     (.propagation)
                     (.injector prop-setter))
        extractor (-> tracing
                      (.propagation)
                      (.extractor prop-getter))
        extract-trace (fn [pc] (.extract extractor pc))
        mk-child-span (fn [span child-name]
                        ;(.annotate (.context span) "sr")
                        (-> tracer
                            (.newChild (.context span))
                            (.name child-name)
                            ;(.annotate "sr")
                            (.start)))]
    (fn [msg-map]
      (let [{:keys [put-fn msg-type msg-meta]} msg-map
            op-name (str msg-type)
            span (if-let [t (:trace msg-meta)]
                   (let [extracted (extract-trace t)]
                     (-> tracer
                         (.joinSpan (.context extracted))
                         (.annotate "sr"))
                     (mk-child-span extracted op-name))
                   (-> tracer
                       (.newTrace)
                       (.name op-name)
                       (.start)))
            res (f (merge msg-map
                          {:span          span
                           :mk-child-span mk-child-span}))
            c (transient {})
            _ (.inject injector (.context span) c)
            pc (persistent! c)
            extracted (extract-trace pc)]
        (.finish span)
        res))))


(defn trace-cmp
  [cmp-map]
  (let [service-name (str (:cmp-id cmp-map))
        tracing (-> (Tracing/newBuilder)
                    (.localServiceName service-name)
                    (.reporter reporter)
                    (.build))
        tracer (.tracer tracing)
        injector (-> tracing
                     (.propagation)
                     (.injector prop-setter))
        extractor (-> tracing
                      (.propagation)
                      (.extractor prop-getter))
        extract-trace (fn [pc] (.extract extractor pc))
        serialized-trace (fn [span]
                           (let [c (transient {})
                                 _ (.inject injector (.context span) c)
                                 pc (persistent! c)]
                             pc))
        mk-child-span (fn [span child-name]
                        ;(.annotate (.context span) "sr")
                        (-> tracer
                            (.newChild (.context span))
                            (.name child-name)
                            ;(.annotate "sr")
                            (.start)))
        state-fn (:state-fn cmp-map)
        wrapped-state-fn
        (fn [put-fn]
          (let [wrapped-put-fn
                (fn [m]
                  (let [msg-meta (or (meta m) {})
                        msg-meta (if (:trace msg-meta)
                                   msg-meta
                                   (let [msg-type (first m)
                                         trace (-> tracer
                                                   (.newTrace)
                                                   (.name (str msg-type " put-fn"))
                                                   (.annotate "cs")
                                                   (.annotate "put-fn")
                                                   (.start))
                                         serialized-trace (serialized-trace trace)]
                                     (.flush trace)
                                     (assoc-in msg-meta [:trace] serialized-trace)))]
                    (put-fn (with-meta m msg-meta))))]
            (when state-fn (state-fn wrapped-put-fn))))
        handler-map (->> (:handler-map cmp-map)
                         (map (fn [[k f]] [k (trace-wrapper f service-name)]))
                         (into {}))]
    (merge cmp-map {:handler-map handler-map
                    :state-fn    wrapped-state-fn})))
