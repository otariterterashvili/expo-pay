import type {
  NativeSyntheticEvent,
  StyleProp,
  ViewProps,
  ViewStyle,
} from "react-native";

export type GooglePayEnvironment = "TEST" | "PRODUCTION";

export type GooglePayButtonTheme = "dark" | "light";

export type GooglePayButtonType =
  | "book"
  | "buy"
  | "checkout"
  | "donate"
  | "ewallet"
  | "order"
  | "pay"
  | "pix"
  | "plain"
  | "subscribe";

export type GooglePayTokenReceivedEvent = {
  token: string;
  paymentData: string;
  paymentMethodData: string;
  paymentMethodType?: string | null;
  cardNetwork?: string;
  cardDetails?: string;
  email?: string;
};

export type GooglePayErrorEvent = {
  code: string;
  message: string;
  nativeMessage?: string;
  statusCode?: number;
  statusMessage?: string | null;
};

export type GooglePayReadyToPayChangedEvent = {
  isReadyToPay: boolean;
};

export type GooglePayJson = string | Record<string, unknown>;

export type ExpoGooglePayModuleEvents = Record<string, never>;

export type GooglePayButtonRef = {
  presentPaymentSheet: () => Promise<void>;
  checkReadiness: () => Promise<void>;
};

export type ExpoGooglePayNativeViewProps = {
  style?: StyleProp<ViewStyle>;
  paymentRequestJson?: string;
  isReadyToPayRequestJson?: string;
  environment?: GooglePayEnvironment;
  buttonTheme?: GooglePayButtonTheme;
  buttonType?: GooglePayButtonType;
  cornerRadius?: number;
  disabled?: boolean;
  existingPaymentMethodRequired?: boolean;
  autoCheckReadiness?: boolean;
  hideWhenNotReady?: boolean;
  onTokenReceived?: (
    event: NativeSyntheticEvent<GooglePayTokenReceivedEvent>,
  ) => void;
  onError?: (event: NativeSyntheticEvent<GooglePayErrorEvent>) => void;
  onCancel?: (event: NativeSyntheticEvent<Record<string, never>>) => void;
  onReadyToPayChanged?: (
    event: NativeSyntheticEvent<GooglePayReadyToPayChangedEvent>,
  ) => void;
};

export type ExpoGooglePayViewProps = Omit<
  ExpoGooglePayNativeViewProps,
  "paymentRequestJson" | "isReadyToPayRequestJson"
> & {
  paymentRequest: GooglePayJson;
  isReadyToPayRequest?: GooglePayJson;
} & ViewProps;
