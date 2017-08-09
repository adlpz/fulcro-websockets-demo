(ns fulcro-ws.app
  (:require [fulcro.client.core :as fc]
            [fulcro.client.data-fetch :as df]
            [fulcro.websockets.networking :as wn]
            [om.dom :as dom]
            [om.next :as om :refer [defui]]
            [fulcro-ws.mutations :as mut]))


;; A Counter element that shows a number and can be clicked to increase it
(defui ^:once Counter
  static fc/InitialAppState
  (initial-state [this {:keys [id start] :as params}]
                 {:counter/id id :counter/n start})
  static om/IQuery
  (query [this] [:counter/id :counter/n])
  static om/Ident
  (ident [this props] [:counter/by-id (:counter/id props)])
  Object
  (render [this]
          (let [{:keys [counter/id counter/n]} (om/props this)
                onClick (om/get-computed this :onClick)]
            (dom/div #js {:className "counter"}
                     (dom/span #js {:onClick #(onClick id)} (str n))))))

(def ui-counter (om/factory Counter {:keyfn :counter/id}))

;; A panel that displays a series of counters
(defui ^:once CounterPanel
  static fc/InitialAppState
  (initial-state [this params]
                 {:counters []})
  static om/IQuery
  (query [this] [{:counters (om/get-query Counter)}])
  static om/Ident
  (ident [this props] [:panels/by-kw :counter])
  Object
  (render [this]
          (let [{:keys [counters]} (om/props this)
                click-callback (fn [id] (om/transact! this `[(mut/counter-inc {:id ~id})]))]
            (dom/div nil
                     (map #(ui-counter (om/computed % {:onClick click-callback})) counters)))))

(def ui-counter-panel (om/factory CounterPanel))

;; One item from the log
(defui ^:once LogItem
  static fc/InitialAppState
  (initial-state [this {:keys [id content] :as params}]
                 {:logitem/id id :logitem/content content})
  static om/IQuery
  (query [this] [:logitem/id :logitem/content])
  static om/Ident
  (ident [this props] [:logitem/by-id (:logitem/id props)])
  Object
  (render [this]
          (let [{:keys [logitem/id logitem/content]} (om/props this)]
            (dom/div nil
                     (dom/strong nil (str id " -> "))
                     (dom/span nil content)))))

(def ui-log-item (om/factory LogItem {:keyfn :logitem/id}))

;; A panel showing a list of log items
(defui ^:once LogPanel
  static fc/InitialAppState
  (initial-state [this params]
                 {:items []})
  static om/IQuery
  (query [this] [{:items (om/get-query LogItem)}])
  static om/Ident
  (ident [this props] [:panels/by-kw :log])
  Object
  (render [this]
          (let [{:keys [items]} (om/props this)]
            (dom/div nil
                     (map #(ui-log-item %) (reverse items))))))

(def ui-log-panel (om/factory LogPanel))

;; The root UI, with all the panels on it
(defui Root
  static fc/InitialAppState
  (initial-state [this params]
                 {:panel-right (fc/get-initial-state LogPanel {})
                  :panel-left (fc/get-initial-state CounterPanel {})})
  static om/IQuery
  (query [this] [:ui/loading-data
                 :ui/react-key
                 {:panel-left (om/get-query CounterPanel)}
                 {:panel-right (om/get-query LogPanel)}])
  Object
  (render [this]
          (let [{:keys [ui/loading-data ui/react-key panel-left panel-right]} (om/props this)]
            (dom/div #js {:key react-key}
                     (if loading-data
                       (dom/span nil "Loading UI...")
                       (dom/div #js {:className "container"}
                                (dom/div #js {:className "row"}
                                         (dom/div #js {:className "col-md-8"}
                                                  (dom/h1 nil "Counters")
                                                  (ui-counter-panel panel-left))
                                         (dom/div #js {:className "col-md-4"}
                                                  (dom/h1 nil "Log")
                                                  (ui-log-panel panel-right)))))))))

;; This is the main change needed client-side in order to enable websockets.
;; We define a networking object to be used in our system
(def cs-net (wn/make-channel-client "/chsk" :global-error-callback (constantly nil)))

;; When building the fulcro client, using the helper new-fulcro-client, we pass the
;; websocket-specific :networking. That's basically it.
(defonce app (atom (fc/new-fulcro-client
                    :networking cs-net
                    :started-callback
                    (fn [{:keys [reconciler] :as app}]
                      (df/load app :counters Counter {:target [:panels/by-kw :counter :counters]}))
                    )))
