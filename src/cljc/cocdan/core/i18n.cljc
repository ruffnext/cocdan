(ns cocdan.core.i18n 
  (:require [tongue.core :as tongue]))

(def inst-strings-en
  {:weekdays-narrow ["S" "M" "T" "W" "T" "F" "S"]
   :weekdays-short  ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
   :weekdays-long   ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
   :months-long     ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["BC" "AD"]
   :eras-long       ["Before Christ" "Anno Domini"]})

(def inst-strings-zh
  {:dayperiods      ["上午" "下午"]})

(def dicts
  {:en
   {:date-full     (tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en)
    :date-short    (tongue/inst-formatter "{month-numeric}/{day}/{year-2digit}" inst-strings-en)}
   
   :zh
   {:date-full-12-hour (tongue/inst-formatter "{year}年{month-numeric}月{day}日{dayperiod}{hour12}时{minutes}分" inst-strings-zh)
    :date-full-24-hour (tongue/inst-formatter "{year}年{month-numeric}月{day}日{hour24}时{minutes}分" inst-strings-zh)

    :dice/waiting-ack "等待骰子确认......"
    :dice/big-failure "大失败！"
    :dice/big-success "大成功！"
    :dice/difficult-success "困难成功"
    :dice/failure "失败"
    :dice/success "成功"
    :dice/unknown "骰子出问题了"
    :dice/very-difficult-success "极难成功！"
    
    :sc/result "San 值减少 {san-loss} 点 ，当前剩余 {san-remain} 点"

    }})

(def translate
  (tongue/build-translate dicts))
