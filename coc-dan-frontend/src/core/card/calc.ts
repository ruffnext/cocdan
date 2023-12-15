export function maxHP (con : number, siz : number) : number {
  return Math.floor((con + siz) / 10)
}

export function maxSan (pow : number) : number {
  return Math.floor(pow)
}

export function maxMP (pow : number) : number {
  return Math.floor(pow / 5)
}