(ns cljs.user
  (:require
    [fulcro-ws.app :refer [app Root]]
    [fulcro.client.core :as fc]))

; so figwheel can call it on reloads. Remounting just forces a UI refresh.
(defn refresh [] (swap! app fc/mount Root "app"))

(defn app-state [] @(:reconciler @app))

(defn log-app-state [& keywords]
  "Dump the current app state"
  (cljs.pprint/pprint (let [app-state (app-state)]
            (if (= 0 (count keywords))
              app-state
              (select-keys app-state keywords)))))


(refresh)
