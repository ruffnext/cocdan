export function deepClone<T> (item : T) : T {
  return JSON.parse(JSON.stringify(item))
}

export function sleep(ms : number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
