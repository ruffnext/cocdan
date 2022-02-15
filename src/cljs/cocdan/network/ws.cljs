(ns cocdan.network.ws 
  (:require [cats.monad.either :as either]
            [cats.core :as m]))

(defn simpleEitherWrapper
  [res]
  (either/branch res
                 #(str %)
                 #(str %)))


(defonce ws-channel (atom nil))

(defn send-transit-msg!
  [msg]
  (simpleEitherWrapper (if @ws-channel
                         (m/mlet [res (either/try-either (.send @ws-channel msg))]
                                 (m/return res))
                         (either/left "WebSocket is not available"))))



(defn make-websocket! [url receive-handler close-handler]
  (simpleEitherWrapper  (m/mlet [channel (either/try-either (js/WebSocket. url))]
                                (do
                                  (set! (.-onmessage channel) #(receive-handler %))
                                  (set! (.-onclose channel) (fn [event]
                                                              (reset! ws-channel nil)
                                                              (close-handler event)))
                                  (reset! ws-channel channel)
                                  (either/right (str "Websocket connection established with:" url))))))