# COC attributes

```json
{
    "age" : 1,
    "gender" : "male/female/other",
    "residence" : "str",
    "homeland" : "str",
    "profession-id" : 1,
    "attrs" : {
        "STR" : 70,
        "DEX" : 70,
        "POW" : 65,
        "CON" : 70,
        "APP" : 40,
        "EDU" : 50,
        "SIZ" : 45,
        "INT" : 50,
        "MOV" : 9,
        "HP"  : 7,
        "STATUS" : "healthy",
        "SAN" : 65,
        "LUCK" : 45,
        "MP" : 13,
        "ARMOR" : 10,
        "ARMOR-COVER" : "str",
        "ARMOR-TYPE" : "str",
        "SAN-LOST-TODAY" : 3,
    },
    "items" : {
        "item-name" : {
            "group" : ["ammo"],
            "weight" : 5,   # kg
            "volume" : 0.5,   # L
            "storage" : "backpack",
            "hidden?" : false
        }
    },
    "background-story" : {
        "appearance-description" : "str",
        "faith" : "str",
        "important-people" : "",
        "place-of-significance" : "",
        "precious-item" : "",
        "characteristic " : "",
        ""
    }
}
```

## weapons
```json 
{
    "weapon-name" : "",
    "applicable-skill" : "skill-name",
    "ammo-name" : ".45",
    "damage" : "1D3+D8",
    "range" : 20,
    "puncture?" : true,
    "frequency" : 1,
    "reload-capacity" : 0,
    "fault-value" : 0
}
```