const mockIsReadyToPayAsync = jest.fn();

jest.mock("expo", () => ({
  requireNativeView: jest.fn(),
  requireOptionalNativeModule: jest.fn(() => ({
    isReadyToPayAsync: mockIsReadyToPayAsync,
  })),
}));

jest.mock("react-native", () => ({
  Platform: {
    OS: "android",
  },
}));

const {
  default: GooglePayButton,
  GooglePayButton: NamedGooglePayButton,
  isReadyToPayAsync: exportedIsReadyToPayAsync,
} = require("../index") as typeof import("../index");
const { isReadyToPayAsync } =
  require("../GooglePay") as typeof import("../GooglePay");
const { serializeGooglePayJson } =
  require("../GooglePayJson") as typeof import("../GooglePayJson");

describe("Google Pay JS API", () => {
  beforeEach(() => {
    mockIsReadyToPayAsync.mockReset();
  });

  it("serializes Google Pay request JSON objects", () => {
    expect(serializeGooglePayJson({ apiVersion: 2, apiVersionMinor: 0 })).toBe(
      '{"apiVersion":2,"apiVersionMinor":0}',
    );
  });

  it("passes string Google Pay request JSON through unchanged", () => {
    expect(serializeGooglePayJson('{"apiVersion":2}')).toBe('{"apiVersion":2}');
  });

  it("exports the stable public API shape", () => {
    expect(GooglePayButton).toBe(NamedGooglePayButton);
    expect(exportedIsReadyToPayAsync).toBe(isReadyToPayAsync);
  });

  it("defaults readiness checks to the TEST environment", async () => {
    mockIsReadyToPayAsync.mockResolvedValueOnce(true);

    await expect(
      isReadyToPayAsync({ apiVersion: 2, apiVersionMinor: 0 }),
    ).resolves.toBe(true);

    expect(mockIsReadyToPayAsync).toHaveBeenCalledWith(
      '{"apiVersion":2,"apiVersionMinor":0}',
      "TEST",
    );
  });

  it("forwards explicit PRODUCTION readiness checks", async () => {
    mockIsReadyToPayAsync.mockResolvedValueOnce(false);

    await expect(isReadyToPayAsync("{}", "PRODUCTION")).resolves.toBe(false);

    expect(mockIsReadyToPayAsync).toHaveBeenCalledWith("{}", "PRODUCTION");
  });
});
