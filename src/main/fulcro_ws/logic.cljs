(ns fulcro-ws.logic)

;; There isn't much here: this is the client-side logic of the applcation,
;; decoupled from the specifics of app state or mutations.

(defn increment-counter
  "Increment a counter"
  [counter] (update counter :counter/n inc))
