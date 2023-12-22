const dict = {
  investor : {
    title   : "Investor",
    name    : "Name",
    gender  : "Gender",
    genderEnum : {
      Other : "Other",
      Male  : "Male",
      Female: "Female"
    },
    age     : "Age",
    career  : "Career",
    homeland: "Homeland"
  },
  characteristic : {
    title : "Attribute",
    str   : "STR",
    dex   : "DEX",
    pow   : "POW",
    con   : "CON",
    app   : "APP",
    edu   : "EDU",
    siz   : "SIZ",
    int   : "INT",
    mov   : "MOV",
    luk   : "LUC"
  },
  status : {
    hp    : "HP",
    san   : "Sanity",
    luk   : "Luck",
    mp    : "MP",
    arm   : "Armor",
    statusHp : "Status",
    statusHpHealthy : "Healthy",
    statusSan : "Status",
    statusSanLucid : "Lucid",
    luckUsed : "Used",
    mpRecovery : "Rec / h",
  },
  healthStatus : {
    Healthy : "Healthy",
    Ill : "Ill",
    Injured : "Injured",
    Critical : "Critical",
    Dead : "Dead"
  },
  mentalStatus : {
    Lucid : "Lucid",
    Fainting : "Fainting",
    TemporaryInsanity : "TempInsane",
    IndefiniteInsanity : "IndefInsane",
    PermanentInsanity : "PermInsane"
  },
  occupation : {
    "Accountant" : {
      name : "Accountant"
    },
    "Secretary" : {
      "name" : "Secretary"
    }
  },
  occupationalSkillEditor : {
    title : "Occupational Skills",
    remain : (num : number) => `(Remain : ${num.toFixed(0)})`,
    reset : "Reset"
  },
  additionalOccupationalSkillEditor : {
    select : (num : number, category : string) => `Select additional ${num.toFixed(0)} ${category} skills`
  },
  interestSkillEditor : {
    title : "Interest Skills",
    remain : (num : number) => `(Remain : ${num.toFixed(0)})`,
    reset : "Reset",
    add : "Add"
  },
  fightingSkillEditor : {
    title : "Fighting Skills"
  },
  weaponEditor : {
    title: "Weapon List",
    name: "Name",
    type: "Type",
    skill: "Use Skills",
    successRate: "Success rate",
    damage: "Damage",
    range: "Range",
    impale: "Puncture",
    rate_of_fire: "Rate Of Fire",
    ammo_capacity: "Ammunition loading capacity",
    reliability: "Reliability"
  }
}

export default dict