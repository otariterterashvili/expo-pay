import { requireNativeView } from "expo";
import * as React from "react";
import { Platform } from "react-native";

import type {
  ExpoGooglePayNativeViewProps,
  ExpoGooglePayViewProps,
  GooglePayButtonRef,
} from "./ExpoPay.types";
import { serializeGooglePayJson } from "./GooglePayJson";

type NativeViewProps = ExpoGooglePayNativeViewProps & {
  ref?: React.Ref<GooglePayButtonRef>;
};

let NativeView: React.ComponentType<NativeViewProps> | null = null;

function getNativeView() {
  if (NativeView == null) {
    NativeView = requireNativeView<NativeViewProps>("ExpoGooglePay");
  }

  return NativeView;
}

const ExpoGooglePayView = React.forwardRef<
  GooglePayButtonRef,
  ExpoGooglePayViewProps
>(function GooglePayButton(
  {
    paymentRequest,
    isReadyToPayRequest,
    environment = "TEST",
    buttonTheme = "dark",
    buttonType = "buy",
    cornerRadius = 4,
    autoCheckReadiness = true,
    ...rest
  },
  ref,
) {
  if (Platform.OS !== "android") {
    throw new Error("GooglePayButton is only available on Android.");
  }

  const NativeGooglePayView = getNativeView();

  return (
    <NativeGooglePayView
      {...rest}
      autoCheckReadiness={autoCheckReadiness}
      buttonTheme={buttonTheme}
      buttonType={buttonType}
      cornerRadius={cornerRadius}
      environment={environment}
      isReadyToPayRequestJson={
        isReadyToPayRequest == null
          ? undefined
          : serializeGooglePayJson(isReadyToPayRequest)
      }
      paymentRequestJson={serializeGooglePayJson(paymentRequest)}
      ref={ref}
    />
  );
});

ExpoGooglePayView.displayName = "GooglePayButton";

export default ExpoGooglePayView;
