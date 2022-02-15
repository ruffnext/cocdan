(ns cocdan.core.indexeddb
  (:require
   [reagent.core :as r]
   [re-posh.core :as rp]))

(defonce idb (r/atom nil))

(defn idb-request-error [e]
  (.error js/console "it raised error when i request IndexedDB" e))

(defn append-message
  [db msg]
  (let [storage (-> db
                    (.transaction "messages" "readwrite")
                    (.objectStore "messages"))
        add-fn (fn [x] (.add storage (clj->js x)))]
    (if (map? msg)
      (add-fn msg)
      (doall (for [x (seq msg)]
               (add-fn x))))))


(defn query-all-messages
  [db callback]
  (let [request (-> db
                 (.transaction  #js ["messages"] "readonly")
                 (.objectStore "messages")
                 .openCursor)
        res (r/atom [])]
    (set! (.-onerror request) idb-request-error)
    (set! (.-onsuccess request)
          (fn [e] 
             (if-let [cursor (-> e .-target .-result)]
              (do 
                (swap! res #(conj % (-> (.-value cursor)
                                        js->clj)))
                (.continue cursor))
              (callback @res))))))

(defn- on-initialize-done
  [db]
  (reset! idb db)
  (query-all-messages db (fn [x]
                           (rp/dispatch [:rpevent/upsert :message x]))))

(defn create-object-store [db]
  (.createObjectStore db "messages" #js {:autoIncrement true}))


(when (nil? @idb)
  (let [request (.open
                 (. js/window -indexedDB)
                 "msg-history" 1)]
    (set! (.-onerror request) idb-request-error)
    (set! (.-onsuccess request) #(on-initialize-done (-> % .-target .-result)))
    (set! (.-onupgradeneeded request) (fn [e]
                                        (let [db (-> e .-target .-result)]
                                          (create-object-store db))))
    (reset! idb request)))
