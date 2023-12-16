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
  attribute : {
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
    IntermittentInsanity : "InterInsane",
    PermanentInsanity : "PermInsane"
  },
  occupation : {
    Accountant : {
      name : "Accountant"
    }
  },
  occupationalSkillEditor : {
    title : "Occupational Skills",
    remain : (num : number) => `(Remain : ${num.toFixed(0)})`
  }
}

export default dict