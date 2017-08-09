(ns fulcro-ws.mutations
  (:require [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.websockets.networking :as wn]
            [fulcro-ws.logic :as l]
            [om.next :as om]))

;; The following are the usual om.next client-side mutations

(defmutation counter-inc
  "Mutation: Increment counter by 1"
  [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state update-in [:counter/by-id id] l/increment-counter))
  (remote [env] true))

(defmutation counter-inc-from-server
  "Mutation triggered by server message: Increment counter by 1"
  [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state update-in [:counter/by-id id] l/increment-counter)))

(defmutation log
  "Mutation: Add item to log"
  [{:keys [content]}]
  (action [{:keys [state]}]
          (let [logitems (:logitem/by-id @state)
                last-id (if (map? logitems)
                          (do (js/console.log logitems) (apply max (keys logitems)))
                          0)
                next-id (inc last-id)]
            (swap! state assoc-in [:logitem/by-id next-id] {:logitem/id next-id :logitem/content content})
            (swap! state update-in [:panels/by-kw :log :items] conj [:logitem/by-id next-id]))))

;; The following are the 'mutations' that are run as a result of a message being pushed
;; from the server side through the websocket. This are just multimethod implementations
;; or fulcro.websockets.networking/push-received and are very similar to normal mutations
;;
;; In our case we respond to those mutations by calling other client-side mutations that
;; will produce the needed side effects on the UI.

(defmethod wn/push-received :user/entered [{:keys [reconciler] :as app} {:keys [topic msg] :as message}]
  (om/transact! reconciler `[(fulcro-ws.mutations/log {:content ~(str "User " (:user msg) " entered")})]))

(defmethod wn/push-received :user/left [{:keys [reconciler] :as app} {:keys [topic msg] :as message}]
  (om/transact! reconciler `[(fulcro-ws.mutations/log {:content ~(str "User " (:user msg) " left")})]))

(defmethod wn/push-received :server/counter-inc [{:keys [reconciler] :as app} {:keys [topic msg] :as message}]
  (let [{:keys [id]} msg]
    (om/transact! reconciler `[(fulcro-ws.mutations/log {:content ~(str "Counter ID " id " incremented by another user")})])
    (om/transact! reconciler `[(fulcro-ws.mutations/counter-inc-from-server {:id ~id})])))
