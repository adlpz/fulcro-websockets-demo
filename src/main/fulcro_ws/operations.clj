(ns fulcro-ws.operations
  (:require
   [fulcro.server :as server :refer [defquery-root defquery-entity defmutation]]
   [fulcro.websockets.protocols :refer [push]]
   [taoensso.timbre :as timbre]
   [om.next :as om]))

;; In this file are the functions that controls the state and mutations of the
;; server-side 'database', which is just an atom with a series of counters, matching
;; the client-side counters. This data could just be completely different and be
;; formatted for the client-side either here or in the same client.

(def server-db (atom {:counters {1 {:counter/id 1 :counter/n 0}
                                  2 {:counter/id 2 :counter/n 1}
                                  3 {:counter/id 3 :counter/n 0}
                                  4 {:counter/id 4 :counter/n 1}
                                  5 {:counter/id 5 :counter/n 0}
                                  6 {:counter/id 6 :counter/n 1}}}))

;; This is the query that is triggered by the client on load. Just pass back the
;; state of all the counters
(defquery-root :counters
  "Queries for the server-side state of the counters"
  (value [env params]
         (-> (get @server-db :counters) vals vec)))

;; The following are some helper functions that pass messages to the clients

(defn notify-all [ws-net verb edn]
  "Send a message with verb :verb and payload :edn to all clients"
  (let [clients (:any @(:connected-cids ws-net))]
    (doall (map (fn [id]
                  (push ws-net id verb edn))
                clients))))

(defn notify-others [ws-net cid verb edn]
  "Send a message with verb :verb and payload :edn to all clients but :cid"
  (let [clients (:any @(:connected-cids ws-net))]
    (doall (map (fn [id]
                  (push ws-net id verb edn)) ;; Use the push protocol function on the ws-net to send to clients.
                (disj clients cid)))))


;; The following are functions to operate on the client side 'database'

(defn remove-user-to-db
  "Remove a user from the list of users"
  [cid]
  (swap! server-db update :users (fn [users] (vec (remove #(= cid %) users)))))

(defn add-user
  "Add a user to the list of users"
  [cid]
  (swap! server-db update :users conj cid))

;; This is the handler for the client-side counter increment mutation. It just logs, incremets
;; the same counter in the server 'database' and triggers sending a message to all other clients
;; indicating that the counter has been incremented.
(defmethod server/server-mutate 'fulcro-ws.mutations/counter-inc [{:keys [ws-net cid] :as env} k {:keys [id] :as params}]
  (timbre/info (str "Incremented counter ID " id))
  (notify-others ws-net cid :server/counter-inc {:id id})
  (swap! server-db update-in [:counters id :counter/n] inc))
