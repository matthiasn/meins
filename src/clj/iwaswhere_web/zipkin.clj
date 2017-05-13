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
      (assoc! c k v)
      (prn :setter c k v))))

(def injector
  (-> tracing
      (.propagation)
      (.injector prop-setter)))

(def prop-getter
  (reify brave.propagation.Propagation$Getter
    (get [this c k]
      (prn :getter k (get-in c [k]))
      (get-in c [k]))))

(def extractor
  (-> tracing
      (.propagation)
      (.extractor prop-getter)))

;tracing.close();
;reporter.close();

(defn child-span
  [span child-name]
  (-> tracer
      (.newChild (.context span))
      (.name child-name)
      (.start)))

(defn child-span-ws
  [span child-name]
  (-> ws-tracer
      (.newChild (.context span))
      (.name child-name)
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
                   (child-span extracted (str op-name)))
                 (-> tracer
                     (.newTrace)
                     (.name (str op-name))
                     (.tag "some-tag" "6.36.0")
                     (.start)))
          res (f (merge m {:span span}))
          c (transient {})
          _ (.inject injector (.context span) c)
          pc (persistent! c)
          extracted (extract-trace pc)]
      (prn :carrier pc)
      (prn :extracted extracted)
      (.finish span)
      res)))
