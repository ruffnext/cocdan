import { ICardI18NRaw } from "./def";

const dict : ICardI18NRaw = {
  investor : {
    title   : "调查员信息",
    name    : "姓名",
    gender  : "性别",
    genderEnum : {
      Other : "其他",
      Male  : "男",
      Female: "女"
    },
    age     : "年龄",
    career  : "职业",
    homeland: "家乡"
  },
  attribute : {
    title   : "属性",
    str     : "力量",
    dex     : "敏捷",
    pow     : "意志",
    con     : "体质",
    app     : "外貌",
    edu     : "教育",
    siz     : "体型",
    int     : "智力",
    mov     : "移动",
    luk     : "幸运"
  },
  status : {
    hp      : "体力",
    san     : "理智",
    luk     : "幸运",
    mp      : "魔法",
    arm     : "护甲",
    statusHp: "状态",
    statusHpHealthy : "健康",
    statusSan : "状态",
    statusSanLucid : "清醒",
    luckUsed : "消耗",
    mpRecovery : "恢复",
  },
  healthStatus : {
    Healthy : "健康",
    Ill : "生病",
    Injured : "受伤",
    Critical : "重伤",
    Dead : "死亡"
  },
  mentalStatus : {
    Lucid : "清醒",
    Fainting : "昏迷",
    TemporaryInsanity : "临时疯狂",
    IntermittentInsanity : "不定疯狂",
    PermanentInsanity : "永久疯狂"
  },
  occupation : {
    Accountant : {
      name : "会计师"
    },
    "Secretary" : {
      "name" : "秘书"
    }
  },
  occupationalSkillEditor : {
    title : "本职技能",
    remain : (num : number) => `（剩余： ${num.toFixed(0)} 点）`,
    reset : "重置"
  },
  additionalOccupationalSkillEditor : {
    select : (num : number, category : string) => `选择额外 ${num.toFixed(0)} 项${category}技能`
  },
  "interestSkillEditor" : {
    title : "兴趣技能",
    remain : (num : number) => `（剩余：${num.toFixed(0)} 点）`,
    reset : "重置",
    add : "新增"
  }
}

export default dict