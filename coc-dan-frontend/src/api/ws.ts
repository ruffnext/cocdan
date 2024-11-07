import { ITxEvent } from "../bindings/api/ws/tx/ITxEvent";

function gen_ws_url(rel_path: string): string {
  let protocol = "ws://";
  if (window.location.protocol === "https:") {
    protocol = "wss://";
  }
  const host = window.location.host;
  const res = `${protocol}${host}${rel_path}`;
  return res;
}

const WS_CACHE = new Map<bigint, StageWebsocket>();

export function new_stage_websocket(stage_id: bigint): StageWebsocket {
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
  private stage_id: bigint;
  private on_message: Map<string, (data: any) => void> = new Map();
  constructor(stage_id: bigint) {
    this.ws = new WebSocket(gen_ws_url(`/api/tx/${stage_id}/ws`));
    this.stage_id = stage_id;
    this.ws.onopen = () => {
      this.status = "connected";
    }
    this.ws.onclose = () => {
      this.ws.close();
      WS_CACHE.delete(stage_id);
      this.status = "closed";
    }
    this.ws.onmessage = (e) => {
      const data: ITxEvent = JSON.parse(e.data);
      for (const [_name, callback] of this.on_message) {
        callback(data);
      }
    }
  }
  addMessageListener(name: string, callback: (data: ITxEvent) => void) {
    if (this.status == "closed") {
      throw new Error("websocket not connected");
    }
    this.on_message.set(name, callback);
  }
  close() {
    this.ws.close();
    WS_CACHE.delete(this.stage_id);
    this.status = "closed";
  }
}
