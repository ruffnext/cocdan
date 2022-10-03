(ns cocdan.data.mixin.equipment)

(defprotocol IEquipmentMixIn
  "可以装备物品的 Performer"
  (install
    [this performer solt]
    "将 performer 装备到 solt 上，成功返回一个 right Performer，失败返回 left 原因")

  (uninstall
    [this performer solt])

  (get-equipments
    [this only-visible?]
    "列出所有的装备。only-visible = true 时仅返回可见的装备")

  (get-slots
    [this]
    "列出所有可装备的槽"))