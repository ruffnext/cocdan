export interface IUser {
  id : number
  name : string
  nick_name : string
}

export class User {
  raw : IUser
  constructor(raw : IUser) {
    this.raw = raw
  }
}
