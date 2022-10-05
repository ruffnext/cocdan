(ns cocdan.data.transaction.dice 
  (:require [cats.monad.either :as either]
            [cocdan.data.core :refer [default-update']]
            [cocdan.data.performer.core :refer [get-attr set-attr]]))

(defprotocol IDice
  (get-actor [this] "返回进行骰子投掷的动作者的 id")
  (get-success-level [this] "返回骰子成功状态，枚举 :success :failure :big-success :big-failure :waiting-ack")
  (get-dice-result [this] "返回骰子的结果"))

(defn dice-success-level-ra
  [dice-result attr-val]
  (cond
    (nil? dice-result) :dice/waiting-ack
    (= dice-result 100) :dice/big-failure
    (> dice-result attr-val) :dice/failure
    (= dice-result 1) :dice/big-success
    (< dice-result (quot attr-val 5)) :dice/very-difficult-success
    (< dice-result (quot attr-val 2)) :dice/difficult-success
    (<= dice-result attr-val) :dice/success
    :else :dice/unknown))

(defn dice-success-level-rc
  [dice-result attr-val]
  (cond
    (nil? dice-result) :dice/waiting-ack
    (> dice-result 95) :dice/big-failure
    (> dice-result attr-val) :dice/failure
    (< dice-result 5) :dice/big-success
    (< dice-result (quot attr-val 5)) :dice/very-difficult-success
    (< dice-result (quot attr-val 2)) :dice/difficult-success
    (<= dice-result attr-val) :dice/success
    :else :dice/unknown))

(defn get-is-success
  [success-level]
  (not (contains? #{:dice/big-failure :dice/failure} success-level)))

(defrecord RC [avatar attr attr-val dice-result]
  IDice
  (get-actor [_this] avatar)
  (get-success-level [_this] (dice-success-level-rc dice-result attr-val))
  (get-dice-result [_this] dice-result))

(defrecord RA [avatar attr attr-val dice-result]
  IDice
  (get-success-level [_this] (dice-success-level-ra dice-result attr-val))
  (get-dice-result [_this] dice-result))

(defrecord SC [avatar attr-val dice-result san-loss loss-on-success loss-on-failure]
  IDice
  (get-actor [_this] avatar)
  (get-success-level [_this] (dice-success-level-rc dice-result attr-val))
  (get-dice-result [_this] dice-result))

(defrecord ST [avatar attr-map])

; TODO: dice rh

(defn handle-st-context
  [{ctx :context/props} {{:keys [avatar attr-map]} :props}] 
  (let [avatar-record (get-in ctx [:avatars (keyword (str avatar))])
        ops (reduce (fn [a [k v]]
                      (let [avatar-attr-val (get-attr avatar-record (name k))]
                        (if (= avatar-attr-val v)
                          a (conj a [(keyword (str "avatars." avatar ".props.attrs." (name k))) avatar-attr-val v]))))
                    [] attr-map)]
    (either/right (default-update' ctx ops))))

(defn handle-sc-transaction
  [_ctx {{:keys [avatar attr-val dice-result san-loss loss-on-success loss-on-failure]} :props}]
  (either/right
   (->SC avatar attr-val dice-result san-loss loss-on-success loss-on-failure)))

(defn handle-sc-context
  [{ctx :context/props} {{:keys [avatar san-loss]} :props}]
  (let [avatar-key (keyword (str avatar))
        avatar (get-in ctx [:avatars (keyword (str avatar))])
        san-val (get-attr avatar "san")] 
    (either/right
     (assoc-in ctx [:avatars avatar-key] (set-attr avatar "san" (- san-val san-loss))))))
