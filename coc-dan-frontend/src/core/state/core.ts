import { post } from "../../api/core";
import { StageWebsocket } from "../../api/ws";
import { ITxEvent } from "../../bindings/api/ws/tx/ITxEvent";
import { IAvatarDetail } from "../../bindings/entity/avatar/IAvatarDetail";

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

const LOGS_PER_PAGE = 30

export class GameState {
  private tx_end_index: number = 0
  private tx_begin_index: number = 0
  private txs: Array<ITxEvent> = []
  // avatar_id -> [tx_index, IDetail]
  private avatars: Map<string, Array<[number, IAvatarDetail | undefined]>> = new Map()
  private stage_id: string
  private ws: StageWebsocket
  private onMessage: (logs: Array<IGameLog>) => void;
  public logs: Array<IGameLog> = []
  constructor(stage_id: string, on_message: (logs: Array<IGameLog>) => void) {
    this.stage_id = stage_id
    this.ws = new StageWebsocket(stage_id)
    this.onMessage = on_message
  }

  async init() {
    if (this.tx_end_index != 0) {
      console.error("GameState already initialized")
      return
    }

    const resp = await post('/tx/:stage_id/state', {
      end_tx_index: null,
      count: LOGS_PER_PAGE
    }, {
      stage_id: this.stage_id
    }, true)

    if ("Error" in resp) {
      console.error(resp.Error)
      return
    }

    const fragment = resp.Ok

    this.tx_begin_index = fragment.begin_tx_index
    this.tx_end_index = fragment.begin_tx_index - 1
    for (const avatar_id in fragment.avatars) {
      const avatar_detail = fragment.avatars[avatar_id]!;
      this.avatars.set(avatar_id, [[fragment.begin_tx_index - 1, avatar_detail]])
    }
    for (let i = fragment.logs.length - 1; i >= 0; i--) {
      this.performTx(fragment.logs[i])
    }
    this.tx_end_index = fragment.end_tx_index

    this.ws.addMessageListener("console.log", (data) => { console.log(data) })
    this.ws.addMessageListener('gameLog', (
      (data: ITxEvent) => {
        const res = this.performTx(data)
        if (res !== undefined) {
          this.logs.push(res)
        }
        this.onMessage(this.logs)
      }
    ).bind(this))
  }

  getAvatarByTxId(avatar_id: string, tx_id: number): IAvatarDetail | undefined {
    const versions = this.avatars.get(avatar_id)
    if (versions === undefined) {
      return undefined
    }
    let detail: IAvatarDetail | undefined = undefined;
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

  getTxBeginIndex() {
    return this.tx_begin_index
  }

  getTxEndIndex() {
    return this.tx_end_index
  }

  hasMoreLogs(): boolean {
    return this.tx_begin_index > 1
  }

  async fetchMoreLogs(): Promise<boolean> {
    if (!this.hasMoreLogs()) {
      return false
    }
    const resp = await post('/tx/:stage_id/state', {
      end_tx_index: this.tx_begin_index,
      count: LOGS_PER_PAGE
    }, {
      stage_id: this.stage_id
    }, true)
    if ("Ok" in resp) {
      const fragment = resp.Ok
      if (fragment.end_tx_index != this.tx_begin_index - 1) {
        console.error(`Invalid tx_index: ${fragment.end_tx_index}, expected: ${this.tx_begin_index - 1}`)
        return false
      }
      const current_tx_end_index = this.tx_end_index
      this.tx_begin_index = fragment.begin_tx_index
      this.tx_end_index = fragment.begin_tx_index - 1
      for (const avatar_id in fragment.avatars) {
        const avatar_detail = fragment.avatars[avatar_id]!;
        const versions = this.avatars.get(avatar_id)
        if (versions === undefined) {
          this.avatars.set(avatar_id, [[fragment.begin_tx_index - 1, avatar_detail]])
        } else {
          versions.push([fragment.begin_tx_index - 1, avatar_detail])
        }
      }
      const currentLogs = this.logs
      this.logs = []
      for (let i = fragment.logs.length - 1; i >= 0; i--) {
        this.performTx(fragment.logs[i])
      }
      for (const log of currentLogs) {
        this.logs.push(log)
      }
      this.tx_begin_index = fragment.begin_tx_index
      this.tx_end_index = current_tx_end_index
    }
    return this.hasMoreLogs()
  }
}