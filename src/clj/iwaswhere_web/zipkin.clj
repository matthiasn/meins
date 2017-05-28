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


(def sender2 (OkHttpSender/create "http://127.0.0.1:9411/api/v1/spans"))
(def reporter2 (AsyncReporter/create sender2))

(def ws-tracing (-> (Tracing/newBuilder)
                    (.localServiceName "ws")
                    (.reporter reporter2)
                    (.build)))
(def ws-tracer (.tracer ws-tracing))

(def blink-tracing (-> (Tracing/newBuilder)
                       (.localServiceName "blink")
                       (.reporter reporter2)
                       (.build)))
(def blink-tracer (.tracer blink-tracing))

(def import-tracing (-> (Tracing/newBuilder)
                        (.localServiceName "import")
                        (.reporter reporter2)
                        (.build)))
(def import-tracer (.tracer import-tracing))

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

(defn child-span
  [span child-name]
  ;(.annotate (.context span) "sr")
  (-> tracer
      (.newChild (.context span))
      (.name child-name)
      ;(.annotate "sr")
      (.start)))

(defn child-span-ws
  [span child-name]
  (-> ws-tracer
      (.newChild (.context span))
      (.name child-name)
      (.annotate "cs")
      (.start)))

(defn child-span-blink
  [span child-name]
  (-> blink-tracer
      (.newChild (.context span))
      (.name child-name)
      (.annotate "sr")
      (.start)))

(defn child-span-import
  [span child-name]
  (-> import-tracer
      (.newChild (.context span))
      (.name child-name)
      (.annotate "sr")
      (.start)))

(defn new-trace
  [op-name]
  (-> tracer
      (.newTrace)
      (.name (str op-name))
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

(defn traced [f op-name]
  (fn [m]
    (let [msg-meta (:msg-meta m)
          span (if-let [t (:trace msg-meta)]
                 (let [extracted (extract-trace t)]
                   ;(.annotate extracted "sr")
                   (-> tracer
                       (.joinSpan (.context extracted))
                       (.annotate "sr"))
                   (child-span extracted (str op-name)))
                 (-> tracer
                     (.newTrace)
                     (.name (str op-name))
                     (.tag "new-span" "1.2.3")
                     (.start)))
          res (f (merge m {:span span}))
          c (transient {})
          _ (.inject injector (.context span) c)
          pc (persistent! c)
          extracted (extract-trace pc)]
      (.finish span)
      res)))

(defn traced2 [f op-name]
  (fn [m]
    (let [msg-meta (:msg-meta m)
          span (if-let [t (:trace msg-meta)]
                 (let [extracted (extract-trace t)]
                   ;(.annotate extracted "sr")
                   (-> blink-tracer
                       (.joinSpan (.context extracted))
                       (.annotate "sr"))
                   (child-span-blink extracted (str op-name)))
                 (-> blink-tracer
                     (.newTrace)
                     (.name (str op-name))
                     (.tag "new-span" "1.2.3")
                     (.start)))
          res (f (merge m {:span span}))
          c (transient {})
          _ (.inject injector (.context span) c)
          pc (persistent! c)
          extracted (extract-trace pc)]
      (.finish span)
      res)))

(defn traced3 [f op-name]
  (fn [m]
    (let [msg-meta (:msg-meta m)
          span (if-let [t (:trace msg-meta)]
                 (let [extracted (extract-trace t)]
                   ;(.annotate extracted "sr")
                   (prn op-name)
                   (-> import-tracer
                       (.joinSpan (.context extracted))
                       (.annotate "sr"))
                   (child-span-import extracted (str op-name)))
                 (-> import-tracer
                     (.newTrace)
                     (.name (str op-name))
                     (.tag "new-span" "1.2.3")
                     (.start)))
          res (f (merge m {:span span}))
          c (transient {})
          _ (.inject injector (.context span) c)
          pc (persistent! c)
          extracted (extract-trace pc)]
      (.finish span)
      res)))

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
        extract-trace (fn [pc] (.extract extractor pc))]
    (fn [m]
      (let [op-name (:msg-type m)
            msg-meta (:msg-meta m)
            span (if-let [t (:trace msg-meta)]
                   (let [extracted (extract-trace t)]
                     (-> tracer
                         (.joinSpan (.context extracted))
                         (.annotate "sr"))
                     (child-span-import extracted (str op-name)))
                   (-> tracer
                       (.newTrace)
                       (.name (str op-name))
                       (.start)))
            res (f (merge m {:span span}))
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
