package expo.modules.googlepay

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.Wallet
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import org.json.JSONException

class ExpoGooglePayModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoGooglePay")

    AsyncFunction("isReadyToPayAsync") { requestJson: String, environment: String?, promise: Promise ->
      val request = try {
        IsReadyToPayRequest.fromJson(requestJson)
      } catch (exception: JSONException) {
        promise.reject("ERR_GOOGLE_PAY_INVALID_REQUEST", "Invalid IsReadyToPayRequest JSON.", exception)
        return@AsyncFunction
      }

      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject(Exceptions.MissingActivity())
        return@AsyncFunction
      }

      val paymentsClient = Wallet.getPaymentsClient(
        activity,
        Wallet.WalletOptions.Builder()
          .setEnvironment(parseGooglePayEnvironment(environment))
          .build()
      )

      paymentsClient.isReadyToPay(request)
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            promise.resolve(task.result == true)
          } else {
            promise.reject(
              "ERR_GOOGLE_PAY_READY_TO_PAY",
              task.exception?.localizedMessage ?: "Google Pay readiness check failed.",
              task.exception
            )
          }
        }
    }

    OnActivityResult { _, (requestCode, resultCode, data) ->
      PendingGooglePayPayment.handleActivityResult(requestCode, resultCode, data)
    }

    View(ExpoGooglePayView::class) {
      Events(
        "onTokenReceived",
        "onError",
        "onCancel",
        "onReadyToPayChanged"
      )

      Prop("paymentRequestJson") { view: ExpoGooglePayView, paymentRequestJson: String? ->
        view.paymentRequestJson = paymentRequestJson
      }

      Prop("isReadyToPayRequestJson") { view: ExpoGooglePayView, isReadyToPayRequestJson: String? ->
        view.isReadyToPayRequestJson = isReadyToPayRequestJson
      }

      Prop("environment") { view: ExpoGooglePayView, environment: String? ->
        view.environment = environment
      }

      Prop("buttonTheme") { view: ExpoGooglePayView, buttonTheme: String? ->
        view.buttonTheme = buttonTheme
      }

      Prop("buttonType") { view: ExpoGooglePayView, buttonType: String? ->
        view.buttonType = buttonType
      }

      Prop("cornerRadius") { view: ExpoGooglePayView, cornerRadius: Double? ->
        view.cornerRadius = cornerRadius
      }

      Prop("disabled") { view: ExpoGooglePayView, disabled: Boolean? ->
        view.disabled = disabled == true
      }

      Prop("existingPaymentMethodRequired") { view: ExpoGooglePayView, existingPaymentMethodRequired: Boolean? ->
        view.existingPaymentMethodRequired = existingPaymentMethodRequired == true
      }

      Prop("autoCheckReadiness") { view: ExpoGooglePayView, autoCheckReadiness: Boolean? ->
        view.autoCheckReadiness = autoCheckReadiness != false
      }

      Prop("hideWhenNotReady") { view: ExpoGooglePayView, hideWhenNotReady: Boolean? ->
        view.hideWhenNotReady = hideWhenNotReady == true
      }

      AsyncFunction("presentPaymentSheet") { view: ExpoGooglePayView ->
        view.presentPaymentSheet()
      }

      AsyncFunction("checkReadiness") { view: ExpoGooglePayView ->
        view.checkReadiness()
      }

      OnViewDidUpdateProps { view: ExpoGooglePayView ->
        view.onPropsUpdated()
      }

      OnViewDestroys { view: ExpoGooglePayView ->
        PendingGooglePayPayment.clear(view)
      }
    }
  }
}
