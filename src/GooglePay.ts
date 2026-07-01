import type { GooglePayEnvironment, GooglePayJson } from "./ExpoPay.types";
import ExpoGooglePayModule from "./ExpoPayModule";
import { serializeGooglePayJson } from "./GooglePayJson";

export function isReadyToPayAsync(
  request: GooglePayJson,
  environment: GooglePayEnvironment = "TEST",
): Promise<boolean> {
  return ExpoGooglePayModule.isReadyToPayAsync(
    serializeGooglePayJson(request),
    environment,
  );
}

export { serializeGooglePayJson };
