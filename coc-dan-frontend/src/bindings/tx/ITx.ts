// This file was generated by [ts-rs](https://github.com/Aleph-Alpha/ts-rs). Do not edit this file manually.
import type { IAvatar } from "../IAvatar";
import type { IDice } from "../dice/IDice";
import type { ISpeak } from "./ISpeak";

export type ITx = { "Speak": ISpeak } | { "Dice": IDice } | { "UpdateAvatar": { before: IAvatar, after: IAvatar, } };