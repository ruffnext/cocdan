import { ITxEvent } from "../bindings/api/ws/tx/ITxEvent";
import { sleep } from "../core/utils";

function gen_ws_url(rel_path: string): string {
  let protocol = "ws://";
  if (window.location.protocol === "https:") {
    protocol = "wss://";
  }
  const host = window.location.host;
  const res = `${protocol}${host}${rel_path}`;
  return res;
}

const WS_CACHE = new Map<string, StageWebsocket>();

export function new_stage_websocket(stage_id: string): StageWebsocket {
  const cache = WS_CACHE.get(stage_id);
  if (cache) {
    return cache;
  }
  const ws = new StageWebsocket(stage_id);
  return ws
}

export class StageWebsocket {
  private ws: WebSocket;
  private status: "pending" | "connected" | "closed" = "pending"
  private stage_id: string;
  private on_message: Map<string, (data: any) => void> = new Map();
  private onOpen() {
    this.status = "connected";
  }
  private async onClose() {
    this.ws.close();
    if (this.status != "closed") {
      this.status = "pending";
      console.log("reconnecting");
      await sleep(1000);
      this.ws = new WebSocket(gen_ws_url(`/api/tx/${this.stage_id}/ws`));
      this.ws.onopen = this.onOpen.bind(this);
      this.ws.onclose = this.onClose.bind(this);
      this.ws.onmessage = this.onMessage.bind(this);
    } else {
      WS_CACHE.delete(this.stage_id);
    }
  }
  private onMessage(e: MessageEvent) {
    const data: ITxEvent = JSON.parse(e.data);
    for (const [_name, callback] of this.on_message) {
      callback(data);
    }
  }
  constructor(stage_id: string) {
    this.ws = new WebSocket(gen_ws_url(`/api/tx/${stage_id}/ws`));
    this.stage_id = stage_id;
    this.ws.onopen = this.onOpen.bind(this);
    this.ws.onclose = this.onClose.bind(this);
    this.ws.onmessage = this.onMessage.bind(this);
  }
  addMessageListener(name: string, callback: (data: ITxEvent) => void) {
    if (this.status == "closed") {
      throw new Error("websocket not connected");
    }
    this.on_message.set(name, callback);
  }
  close() {
    this.status = "closed";
    this.ws.close();
    WS_CACHE.delete(this.stage_id);
  }
}
