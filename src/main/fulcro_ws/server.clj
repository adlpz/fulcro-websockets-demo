(ns fulcro-ws.server
  (:require [fulcro.easy-server :as easy]
            [com.stuartsierra.component :as component]
            [fulcro.server :as server]
            [taoensso.timbre :as timbre]
            [fulcro.websockets.components.channel-server :as cs]
            [fulcro.websockets.protocols :refer [WSListener client-dropped add-listener remove-listener push]]
            [fulcro-ws.operations :as op]))

;; In this file the required components for the server are defined

;; This record implements WSListener, which handles what happens when clients are
;; added or removed
(defrecord ChannelListener [channel-server subscriptions]
  WSListener
  (client-dropped [this ws-net cid]
    (op/remove-user-to-db cid)
    (op/notify-others ws-net cid :user/left {:user cid}))
  (client-added [this ws-net cid]
    (op/add-user cid)
    (op/notify-others ws-net cid :user/entered {:user cid}))

  component/Lifecycle
  (start [component]
    (let [component (assoc component
                      :subscriptions (atom {}))]
      (add-listener channel-server component)
      component))
  (stop [component]
    (remove-listener channel-server component)
    (dissoc component :subscriptions :kill-chan)))

(defn make-channel-listener []
  (component/using
    (map->ChannelListener {})
    [:channel-server]))

;; This builds a complete system using the helper fulcro.easy-server/make-fulcro-server
;; The main differences here enabling websocket communication are the :components passed,
;; being the channel listener we just built and the built-in channel server, and the
;; extra-routes, which define the endpoint for the websocket connection and the handler,
;; which in this case its also built-in in fulcro
;;
;; In this project this is called from the dev/user.clj file, it would be called in whatever
;; is the 'main' entry point in a production implementation.
(defn make-system [config-path]
  (easy/make-fulcro-server
   :config-path config-path
   :parser (server/fulcro-parser)
   :parser-injections #{}
   :components {:channel-server (cs/make-channel-server)
                :channel-listener (make-channel-listener)}
   :extra-routes {:routes ["" {["/chsk"] :web-socket}]
                  :handlers {:web-socket cs/route-handlers}}))
