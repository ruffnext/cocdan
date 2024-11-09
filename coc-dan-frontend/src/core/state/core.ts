import { IGameStateFragment } from "../../bindings/api/tx/state/IGameStateFragment";
import { ITxEvent } from "../../bindings/api/ws/tx/ITxEvent";
import { IDetail } from "../../bindings/entity/avatar/IDetail";

export type IAvatarLog = {
  message: string,
  avatarName: string,
  avatarHeader: string,
  avatarId: string,
}

export type ISystemLog = {
  message: string
}

export type IGameLog = {
  "AvatarLog": IAvatarLog
} | {
  "SystemLog": ISystemLog
}

export class GameState {
  private tx_end_index: number = 0
  private tx_begin_index: number = 0
  private txs: Array<ITxEvent> = []
  // avatar_id -> [tx_index, IDetail]
  private avatars: Map<string, Array<[number, IDetail | undefined]>> = new Map()
  public logs: Array<IGameLog> = []
  constructor() { }

  init(fragment: IGameStateFragment) {
    if (this.tx_end_index != 0) {
      console.error("GameState already initialized")
      return
    }
    this.tx_begin_index = fragment.begin_tx_index
    for (const avatar_id in fragment.avatars) {
      const avatar_detail = fragment.avatars[avatar_id]!;
      this.avatars.set(avatar_id, [[this.tx_begin_index, avatar_detail]])
    }
    for (let i = fragment.logs.length - 1; i >= 0; i--) {
      this.performTx(fragment.logs[i])
    }
    this.tx_end_index = fragment.end_tx_index
  }

  getAvatarByTxId(avatar_id: string, tx_id: number): IDetail | undefined {
    const versions = this.avatars.get(avatar_id)
    if (versions === undefined) {
      return undefined
    }
    let detail: IDetail | undefined = undefined;
    let i = 0;
    for (i = 0; i < versions.length; i++) {
      if (versions[i][0] > tx_id) {
        break
      }
      detail = versions[i][1]
    }
    return detail
  }

  performTx(tx: ITxEvent): IGameLog | undefined {
    if (tx.tx_index != this.tx_end_index + 1) {
      console.error(`Invalid tx_index: ${tx.tx_index}, expected: ${this.tx_end_index + 1}`)
      return undefined
    }
    let response: IGameLog | undefined = undefined;

    if ("RolePlay" in tx.action) {
      const avatar = this.getAvatarByTxId(tx.avatar_id, this.tx_end_index)
      if (avatar == undefined) {
        console.error(`Avatar not found: ${tx.avatar_id}`)
        return undefined
      }
      response = {
        "AvatarLog": {
          "message": tx.action.RolePlay.text,
          "avatarName": avatar.name,
          "avatarHeader": avatar.header,
          "avatarId": tx.avatar_id
        }
      }
    } else if ("AvatarAdd" in tx.action) {
      const [_, avatar] = tx.action.AvatarAdd
      this.avatars.set(tx.avatar_id, [[tx.tx_index, avatar]])
      response = {
        "SystemLog": {
          message: `New avatar: ${avatar.name}`
        }
      }
    } else if ("AvatarDel" in tx.action) {
      const existing = this.avatars.get(tx.avatar_id)
      if (existing === undefined) {
        console.error(`Avatar not found: ${tx.avatar_id}`)
        return undefined
      }
      existing.push([tx.tx_index, undefined])
      response = {
        "SystemLog": {
          message: `Avatar deleted`
        }
      }
    } else if ("AvatarModify" in tx.action) {
      const existing = this.avatars.get(tx.avatar_id)
      if (existing === undefined) {
        console.error(`Avatar not found: ${tx.avatar_id}`)
        return undefined
      }
      existing.push([tx.tx_index, tx.action.AvatarModify[2]])
      response = {
        "SystemLog": {
          message: `Avatar modified`
        }
      }
    }

    if (response === undefined) {
      console.error("Unknown tx action")
      return undefined
    }
    this.logs.push(response)
    this.txs.push(tx)
    this.tx_end_index = tx.tx_index
    return response
  }
}