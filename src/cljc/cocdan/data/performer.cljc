(ns cocdan.data.performer)

(defprotocol IHasEquipmentSolt
  "可以装备物品的 Performer"
  (install
    [this performer solt]
    "将 performer 装备到 solt 上，成功返回一个 right Performer，失败返回 left 原因")
  
  (uninstall
   [this performer solt])
  
  (equipments
   [this only-visible?]
   "列出所有的装备。only-visible = true 时仅返回可见的装备")
  
  (solts 
   [this]
   "列出所有可装备的槽"))

(defprotocol ICountable
  "该表演者是可数的。例如一个人由于是独特的，因此不可数，一堆钱是可数的"
  (merge'
   [this another]
   "将同质的表演者合为一个")
  
  (split'
   [this count-n]
   "将同质的表演者拆为两个")
 )

(defprotocol IPerformer
  "舞台上的表演者"

  (role
    [this]
    "返回该演出者的类型，为枚举 {:npc :avatar :item}")

  (props
    [this prop-name]
    "返回该演出者的某项属性。
    这里特指 COC 的属性，例如 POW、STR 等，不区分大小写。
    对于技能，不应区分中英名称，例如 闪避 和 Dodge 和 dodge 等价 。
    返回为一个整数，不低于 0，不高于 1e4。")

  (header
    [this mood]
    "返回演出者的表情差分。其中 mood 为枚举 {:normal :angry :sad :surprise}")

  (image
    [this]
    "返回演出者的图像。与 header 不同的是，image 不规定图像的尺寸")

  (description
    [this]
    "返回演出者的描述。与 excel 卡中的人物介绍栏相同。可为一个富文本，")

  (status
    [this]
    "返回演出者的状态。为一个 list ，常用的状态有 :hidden （还没想好）")

  (controllable?
    [this]
    "返回 bool ，是否可操作") 
  
  (action!
    [this action]
    "对执行操作"))