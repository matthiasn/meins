(ns i-was-where-app.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :get-greeting (fn [db _] (:greeting db)))
(reg-sub :stats (fn [db _] (:stats db)))
