(ns cocdan.core.ops.core
  (:require [cats.core :as m]
            [cats.monad.either :as either]
            [cocdan.aux :as data-aux]
            [malli.core :as spec]))

(defonce transaction-handler (atom {}))
(defonce context-handler (atom nil))

(defn register-context-handler
  "注册处理 context 的函数。一旦新的 context 产生
   需要经过一些处理后存入 datascript。
   [ctx transaction] -> either context/props
   如果 transaction 没有对应的 handler，则认为这
   个 transaction 不会产生新的 context"
  [type-key handler]
  (swap! context-handler #(assoc % type-key handler)))

(defn register-transaction-handler
  "注册 transaction 的处理函数
   [ctx transaction] -> either transaction/props
   这个函数的主要目的是为了附加以下的上下文
   * 可视化（主要在客户端实现）
   * 时变上下文（例如骰子的点数）"
  [type-key handler]
  (swap! transaction-handler #(assoc % type-key handler)))

(def OP-SNAPSHOT "snapshot")
(def OP-UPDATE "update")

;; op --> operation
;; op 分两种：
;;     request:  尚未被服务器验证的 op，这类 op 的 ack 为 false
;;     history:  已经被服务器验证的 op，该 op 的 ack 为 true
;; 一个 op 在本地构建时，需要被赋予一个 id，但是服务器返回的应答 op 的 id 可能与
;; 本地构建的不同。这很好理解，本地认为这个 id 是 3，但是由于服务器负载很重，在这
;; 个 op 抵达前，有另一个玩家发送了 op，此时服务器实际处理时这个 op 的编号，以及
;; 对应的 ctx_id, time 都会发生变化

;; 客户端发送给服务端的 op 中，id, ctx_id, time 和 ack 字段会被忽略
(defrecord transaction
           [id ctx_id user time type props ack])

(defn make-transaction
  ([id ctx_id user time type props ack]
   (transaction. id ctx_id user (or time (data-aux/get-current-time-string)) type props ack))
  ([id ctx_id user time type props]
   (make-transaction id ctx_id user time type props false)))

(defonce hooks (atom {}))

(defn register-find-ctx-by-id
  "注册函数：fn ( stage-id, ctx_id ) -> either ctx
   如果失败，transaction 将不被执行"
  [handler]
  (swap! hooks #(assoc % :find-ctx-by-id handler)))

(defn ctx-generate-ds
  "在上下文中运行 op 指令，并返回 datascript 的指令"
  ([stage-id {:keys [ctx_id] :as op}]
   (let [func (:find-ctx-by-id @hooks)]
     (if func
       (either/branch-right
        (func stage-id ctx_id)
        (fn [ctx] (ctx-generate-ds stage-id op ctx)))
       (either/left "未设置缺省的上下文查询 hook，无法查询上下文"))))
  ([_stage-id {:keys [id ctx_id time type ack] :as t-record} {ctx_id-from-ctx :context/id
                                                              stage-id :context/stage
                                                              _context-ack :context/ack :as ctx}]
   (let [type-key (keyword (str type))
         t-handler (or (type-key @transaction-handler) (fn [_ t-record] (either/right (:props t-record))))
         c-handler (or (type-key @context-handler) (fn [& _] (either/right nil)))] 
     (m/mlet
      [new-t-props (t-handler ctx t-record)
       new-t-record (either/right (assoc t-record :ctx_id (or ctx_id-from-ctx ctx_id) :stage stage-id :props new-t-props :ack (or ack false)))
       c-props (c-handler ctx new-t-record)]
      (either/right
       (if c-props
         [(data-aux/add-db-prefix :transaction new-t-record) (data-aux/add-db-prefix :context {:id id :time time :props c-props :ask (or ack false)})]
         [(data-aux/add-db-prefix :transaction new-t-record)]))))))

(def update-props-spec
  [:vector
   [:tuple keyword? some? some?]])

(def speak-props-spec
  [:map
   [:avatar int?]
   [:message string?]
   [:props associative?]])

(def r-props-spec
  [:map
   [:avatar int?]
   [:attr string?]])

(def st-props-spec
  [:map
   [:avatar int?]
   [:attr-map associative?]])

(def narration-props-spec
  [:map
   [:substage string?]
   [:message string?]
   [:props associative?]])

(def sc-props-spec
  [:map
   [:avatar int?]
   [:loss-on-success string?]
   [:loss-on-failure string?]])

(defn m-final-validate-transaction
  [{:keys [type props]}]
  (let [res (case type
              "update" (spec/explain update-props-spec props)
              "speak" (spec/explain speak-props-spec props)
              "narration" (spec/explain narration-props-spec props)
              "rc" (spec/explain r-props-spec props)
              "ra" (spec/explain r-props-spec props)
              "st" (spec/explain st-props-spec props)
              "sc" (spec/explain sc-props-spec props)
              {:errors (str "无法检验 transaction 类型" type)})]
    (if res
      (either/left res)
      (either/right))))
