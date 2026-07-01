import ExpoGooglePayView from "../ExpoPayView.web";

describe("ExpoGooglePayView web fallback", () => {
  it("fails only when the unsupported component is used", () => {
    expect(() =>
      ExpoGooglePayView({
        paymentRequest: {},
      }),
    ).toThrow("ExpoGooglePayView is not available on the web platform.");
  });
});
