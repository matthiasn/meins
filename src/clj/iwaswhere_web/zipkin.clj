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
(def tracer (.tracer tracing))

(def ws-tracing (-> (Tracing/newBuilder)
                    (.localServiceName "ws")
                    (.reporter reporter)
                    (.build)))
(def ws-tracer (.tracer ws-tracing))

(def prop-setter
  (reify brave.propagation.Propagation$Setter
    (put [this c k v]
      (assoc! c k v))))

(def injector
  (-> tracing
      (.propagation)
      (.injector prop-setter)))

(def prop-getter
  (reify brave.propagation.Propagation$Getter
    (get [this c k]
      (get-in c [k]))))

(def extractor
  (-> tracing
      (.propagation)
      (.extractor prop-getter)))

;tracing.close();
;reporter.close();

(defn child-span-ws
  [span child-name]
  (-> ws-tracer
      (.newChild (.context span))
      (.name child-name)
      (.annotate "cs")
      (.start)))

(defn new-trace-ws
  [op-name]
  (-> ws-tracer
      (.newTrace)
      (.name (str op-name))
      (.annotate "cs")
      (.start)))

(defn serialized-trace
  [span]
  (let [c (transient {})
        _ (.inject injector (.context span) c)
        pc (persistent! c)]
    pc))

(defn extract-trace
  [pc]
  (.extract extractor pc))

(defn trace-wrapper [f cmp-id]
  (let [service-name (str cmp-id)
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
  (let [cmp-id (:cmp-id cmp-map)
        handler-map (->> (:handler-map cmp-map)
                         (map (fn [[k f]] [k (trace-wrapper f cmp-id)]))
                         (into {}))]
    (merge cmp-map {:handler-map handler-map})))
