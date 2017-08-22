(ns iwaswhere-electron.renderer.log)

(enable-console-print!)

(defn info
  [& args]
  (apply println args))

(defn error
  [& args]
  (apply println args))
