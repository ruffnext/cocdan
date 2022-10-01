(ns cocdan.core.ops.core
  (:require [cocdan.data.core :as data-core]
            [cocdan.aux :as data-aux]
            [malli.core :as spec]
            [cats.monad.either :as either]))

(defonce transaction-handler (atom {}))
(defonce context-handler (atom nil))

(defn register-context-handler
  "注册处理 context 的函数。一旦新的 context 产生
   需要经过一些处理后存入 datascript。
   格式为： ctx/props -> ctx-props-ds-record"
  [handler]
  (reset! context-handler handler))

(defn register-transaction-handler
  "注册 transaction 的处理函数
   [ctx transaction] -> transaction/props
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
           [id ctx_id time type props ack])

(defn make-transaction
  ([id ctx_id time type props ack]
   (transaction. id ctx_id (or time (data-aux/get-current-time-string)) type props ack))
  ([id ctx_id time type props]
   (make-transaction id ctx_id time type props false)))

(defonce hooks (atom {}))

(defn register-find-ctx-by-id
  "注册函数：fn ( stage-id, ctx_id ) -> ctx
   当 ctx = nil 时，op 将不执行"
  [handler]
  (swap! hooks #(assoc % :find-ctx-by-id handler)))

(defn op-to-transaction-ds
  [op ack]
  (-> (data-aux/add-db-prefix :transaction op)
      (assoc :transaction/ack ack)))

(defn context-to-context-ds
  [op ack]
  (-> (data-aux/add-db-prefix :context op)
      (assoc :context/ack ack)))

(defn- generate-new-context
  [ctx {:keys [type props]}]
  (cond
    (= type OP-SNAPSHOT) props
    (= type OP-UPDATE) (data-core/update' ctx props)
    :else nil))

(defn ctx-generate-ds
  "在上下文中运行 op 指令，并返回 datascript 的指令"
  ([stage-id {:keys [ctx_id] :as op}]
   (when-let [func (:find-ctx-by-id @hooks)]
     (when-let [ctx (func stage-id ctx_id)]
       (ctx-generate-ds stage-id op ctx))))
  ([_stage-id {:keys [id ctx_id time type ack] :as op} {ctx_id-from-ctx :context/id
                                                        stage-id :context/stage
                                                        _context-ack :context/ack :as ctx}]
   (let [handler ((keyword type) @transaction-handler)
         t-item (-> op
                    (#(if handler
                        (assoc % :props (handler ctx op)) %))
                    (#(assoc % :ctx_id (or ctx_id-from-ctx ctx_id) :stage stage-id)) 
                    (op-to-transaction-ds (or ack false)))] 
     (if (contains? #{"snapshot" "update"} type)
       (let [new-context-props (@context-handler (generate-new-context (:context/props ctx) op))]
         [t-item (context-to-context-ds {:id id :time time :props new-context-props} ack)])
       [t-item]))))

(def update-props-spec
  [:vector
   [:tuple keyword? some? some?]])

(def speak-props-spec
  [:map
   [:avatar pos-int?]
   [:message string?]
   [:props associative?]])

(defn m-final-validate-transaction
  [{:keys [type props]}]
  (let [res (case type
              "update" (spec/explain update-props-spec props)
              "speak" (spec/explain speak-props-spec props)
              {:errors (str "无法识别 transaction 类型 " type)})]
    (if res
      (either/left res)
      (either/right))))
