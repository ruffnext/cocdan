(ns cocdan.data.transaction.dice 
  (:require [cocdan.data.core :refer [default-update']]
            [cocdan.data.performer.core :refer [get-attr]]))

(defprotocol IDice
  (get-actor [this] "返回进行骰子投掷的动作者的 id")
  (get-is-success [this] "返回骰子成功状态，枚举 :success :failure :big-success :big-failure :waiting-ack")
 )

(defrecord RC [avatar attr attr-val dice-result]
  IDice
  (get-actor [_this] avatar)
  (get-is-success [_this] (cond
                            (nil? dice-result) :dice/waiting-ack
                            (= dice-result 100) :dice/big-failure
                            (> dice-result attr-val) :dice/failure
                            (= dice-result 1) :dice/big-success
                            (< dice-result (quot attr-val 5)) :dice/very-difficult-success
                            (< dice-result (quot attr-val 2)) :dice/difficult-success
                            (<= dice-result attr-val) :dice/success
                            :else :dice/unknown)))

(defrecord RA [avatar attr attr-val dice-result]
  IDice
  (get-actor [_this] avatar)
  (get-is-success [_this] (cond
                            (nil? dice-result) :dice/waiting-ack
                            (> dice-result 95 ) :dice/big-failure
                            (> dice-result attr-val) :dice/failure
                            (< dice-result 5) :dice/big-success
                            (< dice-result (quot attr-val 5)) :dice/very-difficult-success
                            (< dice-result (quot attr-val 2)) :dice/difficult-success
                            (<= dice-result attr-val ) :dice/success
                            :else :dice/unknown)))

(defrecord ST [avatar attr-map])

(defn handle-st-context
  [{ctx :context/props} {{:keys [avatar attr-map]} :props}] 
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        ops (reduce (fn [a [k v]]
                      (let [avatar-attr-val (get-attr avatar-record (name k))]
                        (if (= avatar-attr-val v)
                          a (conj a [(keyword (str "avatars." avatar ".props.attrs." (name k))) avatar-attr-val v]))))
                    [] attr-map)]
    (default-update' ctx ops)))
