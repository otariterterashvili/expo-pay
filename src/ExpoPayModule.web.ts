import { NativeModule, registerWebModule } from "expo";

import type {
  ExpoGooglePayModuleEvents,
  GooglePayEnvironment,
} from "./ExpoPay.types";

class ExpoGooglePayModule extends NativeModule<ExpoGooglePayModuleEvents> {
  isReadyToPayAsync(
    _requestJson: string,
    _environment?: GooglePayEnvironment,
  ): Promise<boolean> {
    return Promise.resolve(false);
  }
}

export default registerWebModule(ExpoGooglePayModule, "ExpoGooglePay");
