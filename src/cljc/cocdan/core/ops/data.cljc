(ns cocdan.core.ops.data 
  (:require [cats.monad.either :as either]
            [malli.core :as mc]
            [malli.error :as me]))

(def context-spec
  [:map
   [:id {:title "context-id"} int?] 
   [:time {:title "timestamp"} string?]
   [:payload {:title "payload"} any?]
   [:ack {:title "is stage acknowledged?"} boolean?]])

(def transaction-spec
  [:map
   [:id {:title "transaction-id"} int?]
   [:ctx_id {:title "context-id"} int?]
   [:user {:title "user-id"} int?]
   [:time {:title "timestamp"} string?]
   [:type {:title "type-string"} string?]
   [:payload {:title "payload of transaction"} any?]
   [:ack {:title "is transaction acknowledged?"} boolean?]])

(defn make-either-spec
  [left right]
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (cond
                      (not (either/either? value)) (str "should be an either but got " value)
                      :else (-> (either/branch 
                                 value
                                 (fn [left-val] (mc/explain left left-val))
                                 (fn [right-val] (mc/explain right right-val)))
                                (me/humanize)
                                str)))}
   (fn [x]
     (and (either/either? x)
          (either/branch 
           x
           (fn [left-val] (mc/validate left left-val))
           (fn [right-val] (mc/validate right right-val)))))])
