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

;tracing.close();
;reporter.close();

(def span (-> tracer
              (.newTrace)
              (.name "find")
              (.start)))

(defn traced [f op-name]
  (fn [m]
    (let [span (-> tracer
                   (.newTrace)
                   (.name (str op-name))
                   (.tag "clnt/finagle.version" "6.36.0")
                   (.start))
          res (f (merge m {:span span}))]
      (.finish span)
      res)))

(defn child-span
  [span child-name]
  (-> tracer
      (.newChild (.context span))
      (.name child-name)
      (.start)))