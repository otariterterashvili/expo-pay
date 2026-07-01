import type { GooglePayJson } from "./ExpoPay.types";

export function serializeGooglePayJson(value: GooglePayJson): string {
  return typeof value === "string" ? value : JSON.stringify(value);
}
