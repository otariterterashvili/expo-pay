import { requireOptionalNativeModule, type NativeModule } from "expo";

import type {
  ExpoGooglePayModuleEvents,
  GooglePayEnvironment,
} from "./ExpoPay.types";

export type ExpoGooglePayNativeModule =
  NativeModule<ExpoGooglePayModuleEvents> & {
    isReadyToPayAsync(
      requestJson: string,
      environment?: GooglePayEnvironment,
    ): Promise<boolean>;
  };

const ExpoGooglePayModule =
  requireOptionalNativeModule<ExpoGooglePayNativeModule>("ExpoGooglePay");

const ExpoGooglePayModuleFallback = {
  isReadyToPayAsync: () => Promise.resolve(false),
} as unknown as ExpoGooglePayNativeModule;

export default ExpoGooglePayModule ?? ExpoGooglePayModuleFallback;
