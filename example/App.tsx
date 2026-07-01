import GooglePayButton, {
  isReadyToPayAsync,
  type GooglePayButtonRef,
} from "expo-pay";
import { useRef, useState } from "react";
import {
  Button,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";

const baseCardPaymentMethod = {
  type: "CARD",
  parameters: {
    allowedAuthMethods: ["PAN_ONLY", "CRYPTOGRAM_3DS"],
    allowedCardNetworks: ["AMEX", "DISCOVER", "MASTERCARD", "VISA"],
  },
};

const cardPaymentMethod = {
  ...baseCardPaymentMethod,
  tokenizationSpecification: {
    type: "PAYMENT_GATEWAY",
    parameters: {
      gateway: "example",
      gatewayMerchantId: "exampleGatewayMerchantId",
    },
  },
};

const isReadyToPayRequest: Record<string, unknown> = {
  apiVersion: 2,
  apiVersionMinor: 0,
  allowedPaymentMethods: [baseCardPaymentMethod],
};

const paymentRequest: Record<string, unknown> = {
  apiVersion: 2,
  apiVersionMinor: 0,
  allowedPaymentMethods: [cardPaymentMethod],
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

export default function App() {
  const googlePayRef = useRef<GooglePayButtonRef>(null);
  const [isReadyToPay, setIsReadyToPay] = useState<boolean | null>(null);
  const [lastEvent, setLastEvent] = useState("No payment event yet.");

  const checkReadiness = async () => {
    const result = await isReadyToPayAsync(isReadyToPayRequest);
    setIsReadyToPay(result);
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Android Google Pay</Text>
        <Text style={styles.copy}>
          This example uses the Google Pay TEST environment with a sample
          gateway tokenization request.
        </Text>

        <View style={styles.section}>
          <Text style={styles.label}>Readiness</Text>
          <Text style={styles.value}>
            {isReadyToPay == null ? "Not checked" : String(isReadyToPay)}
          </Text>
          <Button title="Check readiness" onPress={checkReadiness} />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Payment</Text>
          {Platform.OS === "android" ? (
            <>
              <GooglePayButton
                ref={googlePayRef}
                style={styles.googlePayButton}
                paymentRequest={paymentRequest}
                isReadyToPayRequest={isReadyToPayRequest}
                environment="TEST"
                buttonTheme="dark"
                buttonType="buy"
                cornerRadius={6}
                hideWhenNotReady
                onReadyToPayChanged={({ nativeEvent }) => {
                  setIsReadyToPay(nativeEvent.isReadyToPay);
                }}
                onTokenReceived={({ nativeEvent }) => {
                  setLastEvent(
                    `Token received: ${nativeEvent.token.slice(0, 40)}...`,
                  );
                }}
                onCancel={() => {
                  setLastEvent("Payment canceled.");
                }}
                onError={({ nativeEvent }) => {
                  setLastEvent(`${nativeEvent.code}: ${nativeEvent.message}`);
                }}
              />
              <Button
                title="Present with ref"
                onPress={() => googlePayRef.current?.presentPaymentSheet()}
              />
            </>
          ) : (
            <Text style={styles.value}>
              Google Pay is only available on Android in this package.
            </Text>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Last event</Text>
          <Text style={styles.value}>{lastEvent}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f5f7fb",
  },
  content: {
    gap: 16,
    padding: 20,
  },
  title: {
    color: "#111827",
    fontSize: 28,
    fontWeight: "700",
  },
  copy: {
    color: "#4b5563",
    fontSize: 15,
    lineHeight: 22,
  },
  section: {
    gap: 10,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 16,
  },
  label: {
    color: "#111827",
    fontSize: 16,
    fontWeight: "700",
  },
  value: {
    color: "#374151",
    fontSize: 14,
    lineHeight: 20,
  },
  googlePayButton: {
    height: 48,
    width: "100%",
  },
});
