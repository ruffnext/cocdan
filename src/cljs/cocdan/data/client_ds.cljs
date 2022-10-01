(ns cocdan.data.client-ds 
  (:require [cocdan.data.avatar :refer [Avatar]]
            [cocdan.data.stage :refer [Stage]]))

(defprotocol IClientDsRecord
  (to-ds [this] "客户端中，将数据保存到 DataScript 中便于其他地方查询"))

(extend-type Avatar
  IClientDsRecord
  (to-ds [this] {:avatar/id (:id this)
                 :avatar/props this}))

(extend-type Stage
  IClientDsRecord
  (to-ds [this] {:stage/id (:id this)
                 :stage/props this}))