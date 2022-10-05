(ns cocdan.data.performer.core)

;; 所有存在于舞台上的东西都是 performer

(defprotocol ICountable
  "该表演者是可数的。例如一个人由于是独特的，因此不可数，一堆钱是可数的"
  (merge'
    [this another]
    "将同质的表演者合为一个")

  (split'
    [this count-n]
    "将同质的表演者拆为两个"))

(defprotocol IPerformer
  "舞台上的表演者"

  (get-role
    [this]
    "返回该演出者的类型，为枚举 {:npc :avatar :item}")

  (get-attr
    [this attr-name]
    "返回该演出者的某项属性。
    这里特指 COC 的属性，例如 POW、STR 等，不区分大小写。
    对于技能，不应区分中英名称，例如 闪避 和 Dodge 和 dodge 等价 。
    返回为一个整数，不低于 0，不高于 1e4。")

  (set-attr
    [this attr-name attr-val])

  (get-attr-max
    [this attr-name]
    "返回该演出者的某项属性的最大值")

  (get-header
    [this mood]
    "返回演出者的表情差分。其中 mood 为枚举 {:normal :angry :sad :surprise}")

  (get-image
    [this]
    "返回演出者的图像。与 get-header 不同的是，image 不规定图像的尺寸")

  (get-description
    [this]
    "返回演出者的描述。与 excel 卡中的人物介绍栏相同。可为一个富文本，")

  (get-status
    [this]
    "返回演出者的状态。为一个 list ，常用的状态有 :hidden （还没想好）")

  (controllable?
    [this]
    "返回 bool ，是否可操作")

  (action!
    [this action]
    "对执行操作"))
