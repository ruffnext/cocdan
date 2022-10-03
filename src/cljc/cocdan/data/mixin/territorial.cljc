(ns cocdan.data.mixin.territorial)

"表示具有地点属性的数据"

(defprotocol ITerritorialMixIn
  (get-substage-id [this] "返回该发生的地点"))