# expo-pay

Android Google Pay for Expo native apps.

`expo-pay` provides an Expo native module and native view for rendering the
official Google Pay button, checking `isReadyToPay`, and presenting the Android
Google Pay payment sheet. It does not process payments or talk to your backend:
you provide the Google Pay request JSON for your gateway, and your server or
payment provider handles the returned token.

Apple Pay is intentionally not implemented yet.

## Installation

```sh
npm install expo-pay
```

This package contains Android native code, so it must be used in an Expo
development build or a prebuilt/bare React Native app. It will not run inside
Expo Go.

```sh
npx expo prebuild
npx expo run:android
```

The Android module includes the Google Pay manifest metadata required by
Google:

```xml
<meta-data
  android:name="com.google.android.gms.wallet.api.enabled"
  android:value="true" />
```

## Basic Usage

```tsx
import GooglePayButton, { isReadyToPayAsync } from "expo-pay";
import { useRef, useState } from "react";
import type { GooglePayButtonRef } from "expo-pay";

const baseCardPaymentMethod = {
  type: "CARD",
  parameters: {
    allowedAuthMethods: ["PAN_ONLY", "CRYPTOGRAM_3DS"],
    allowedCardNetworks: ["AMEX", "DISCOVER", "MASTERCARD", "VISA"],
  },
};

const paymentRequest = {
  apiVersion: 2,
  apiVersionMinor: 0,
  allowedPaymentMethods: [
    {
      ...baseCardPaymentMethod,
      tokenizationSpecification: {
        type: "PAYMENT_GATEWAY",
        parameters: {
          gateway: "example",
          gatewayMerchantId: "exampleGatewayMerchantId",
        },
      },
    },
  ],
  merchantInfo: {
    merchantName: "Example Merchant",
  },
  transactionInfo: {
    totalPriceStatus: "FINAL",
    totalPrice: "10.00",
    currencyCode: "USD",
    countryCode: "US",
  },
};

const isReadyToPayRequest = {
  apiVersion: 2,
  apiVersionMinor: 0,
  allowedPaymentMethods: [baseCardPaymentMethod],
};

export function Checkout() {
  const ref = useRef<GooglePayButtonRef>(null);
  const [ready, setReady] = useState<boolean | null>(null);

  async function checkReadiness() {
    setReady(await isReadyToPayAsync(isReadyToPayRequest));
  }

  return (
    <GooglePayButton
      ref={ref}
      style={{ height: 48, width: "100%" }}
      paymentRequest={paymentRequest}
      isReadyToPayRequest={isReadyToPayRequest}
      environment="TEST"
      buttonTheme="dark"
      buttonType="buy"
      onReadyToPayChanged={({ nativeEvent }) => {
        setReady(nativeEvent.isReadyToPay);
      }}
      onTokenReceived={({ nativeEvent }) => {
        // Send nativeEvent.token to your backend/payment gateway.
      }}
      onCancel={() => {}}
      onError={({ nativeEvent }) => {
        console.warn(nativeEvent.code, nativeEvent.message);
      }}
    />
  );
}
```

## API

### `isReadyToPayAsync(request, environment?)`

Checks whether Google Pay is available for the supplied request.

- `request`: raw Google Pay `IsReadyToPayRequest` JSON as a string or object.
- `environment`: `"TEST"` or `"PRODUCTION"`. Defaults to `"TEST"`.
- Returns `false` on unsupported platforms.

### `GooglePayButton`

Renders the native Google Pay button and presents the payment sheet when
pressed.

Props:

- `paymentRequest`: required Google Pay `PaymentDataRequest` JSON as a string or
  object.
- `isReadyToPayRequest`: optional explicit readiness request. If omitted, the
  module derives one from `paymentRequest.allowedPaymentMethods`.
- `environment`: `"TEST"` or `"PRODUCTION"`. Defaults to `"TEST"`.
- `buttonTheme`: `"dark"` or `"light"`.
- `buttonType`: `"book"`, `"buy"`, `"checkout"`, `"donate"`, `"ewallet"`,
  `"order"`, `"pay"`, `"pix"`, `"plain"`, or `"subscribe"`.
- `cornerRadius`: native Google Pay button corner radius.
- `disabled`: disables button presses.
- `existingPaymentMethodRequired`: adds
  `existingPaymentMethodRequired: true` to the derived readiness request.
- `autoCheckReadiness`: checks readiness after prop updates. Defaults to `true`.
- `hideWhenNotReady`: hides the button when readiness is `false`.

Events:

- `onReadyToPayChanged`: `{ isReadyToPay }`
- `onTokenReceived`: `{ token, paymentData, paymentMethodData, paymentMethodType,
cardNetwork, cardDetails, email }`
- `onCancel`
- `onError`: `{ code, message, nativeMessage, statusCode, statusMessage }`

Ref methods:

- `presentPaymentSheet(): Promise<void>`
- `checkReadiness(): Promise<void>`

## TEST and PRODUCTION

Use `"TEST"` while developing. Google's TEST environment returns fake,
non-chargeable payment credentials and does not require a Google Pay merchant
approval.

Before using `"PRODUCTION"`:

- Replace the example gateway values with your payment processor values.
- Use your real Google Pay merchant name and merchant ID where required.
- Complete Google's production access and integration checklist.
- Distribute the Android app through a production-signed build; Google Pay
  production integrations are reviewed by Google.

Google references:

- [Android setup](https://developers.google.com/pay/api/android/guides/setup)
- [Android tutorial](https://developers.google.com/pay/api/android/guides/tutorial)
- [Test and deploy](https://developers.google.com/pay/api/android/guides/test-and-deploy/integration-checklist)

Expo references:

- [Native module tutorial](https://docs.expo.dev/modules/native-module-tutorial/)
- [Native view tutorial](https://docs.expo.dev/modules/native-view-tutorial/)
- [Module config](https://docs.expo.dev/modules/module-config/)

## Troubleshooting

`Cannot find native module 'ExpoGooglePay'`

Rebuild the native app after installing the package:

```sh
npx expo prebuild
npx expo run:android
```

`GooglePayButton is only available on Android`

This package currently implements Android Google Pay only. Guard rendering with
`Platform.OS === "android"` in shared screens.

`ERR_GOOGLE_PAY_INVALID_REQUEST`

The request JSON could not be parsed by Google Pay. Validate the shape against
Google's `PaymentDataRequest` and `IsReadyToPayRequest` docs.

`ERR_GOOGLE_PAY_READY_TO_PAY`

The readiness check failed at the Google Pay API layer. Check Google Play
services availability, Android emulator/device configuration, and request JSON.
