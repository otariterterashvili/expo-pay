@file:Suppress("DEPRECATION")

package expo.modules.googlepay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Locale

private const val GOOGLE_PAY_REQUEST_CODE = 29731

@SuppressLint("ViewConstructor")
class ExpoGooglePayView(context: android.content.Context, appContext: AppContext) : ExpoView(context, appContext) {
  internal val onTokenReceived by EventDispatcher<Map<String, Any?>>()
  internal val onError by EventDispatcher<Map<String, Any?>>()
  internal val onCancel by EventDispatcher<Unit>()
  internal val onReadyToPayChanged by EventDispatcher<Map<String, Any?>>()

  private val payButton = PayButton(context)
  private var needsButtonConfiguration = true
  private var needsReadinessCheck = true
  private var paymentInProgress = false

  var paymentRequestJson: String? = null
    set(value) {
      field = value
      needsButtonConfiguration = true
      needsReadinessCheck = true
    }

  var isReadyToPayRequestJson: String? = null
    set(value) {
      field = value
      needsReadinessCheck = true
    }

  var environment: String? = null
    set(value) {
      field = value
      needsReadinessCheck = true
    }

  var buttonTheme: String? = null
    set(value) {
      field = value
      needsButtonConfiguration = true
    }

  var buttonType: String? = null
    set(value) {
      field = value
      needsButtonConfiguration = true
    }

  var cornerRadius: Double? = null
    set(value) {
      field = value
      needsButtonConfiguration = true
    }

  var disabled = false
    set(value) {
      field = value
      payButton.isEnabled = !value
      alpha = if (value) 0.6f else 1f
    }

  var existingPaymentMethodRequired = false
    set(value) {
      field = value
      needsReadinessCheck = true
    }

  var autoCheckReadiness = true
    set(value) {
      field = value
      needsReadinessCheck = true
    }

  var hideWhenNotReady = false
    set(value) {
      field = value
      if (!value) {
        payButton.visibility = View.VISIBLE
      }
      needsReadinessCheck = true
    }

  init {
    clipToOutline = true
    payButton.layoutParams = FrameLayout.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT
    )
    payButton.setOnClickListener {
      presentPaymentSheet()
    }
    addView(payButton)
  }

  fun onPropsUpdated() {
    if (needsButtonConfiguration) {
      configureButton()
      needsButtonConfiguration = false
    }

    if (autoCheckReadiness && needsReadinessCheck) {
      checkReadiness()
      needsReadinessCheck = false
    }
  }

  fun presentPaymentSheet() {
    if (disabled) {
      return
    }

    if (paymentInProgress) {
      emitError("ERR_GOOGLE_PAY_ALREADY_IN_PROGRESS", "A Google Pay request is already in progress.", null)
      return
    }

    val activity = try {
      appContext.throwingActivity
    } catch (exception: Exceptions.MissingActivity) {
      emitError("ERR_GOOGLE_PAY_MISSING_ACTIVITY", exception.localizedMessage ?: "The current activity is unavailable.", exception)
      return
    }

    if (!PendingGooglePayPayment.begin(this)) {
      emitError("ERR_GOOGLE_PAY_ALREADY_IN_PROGRESS", "A Google Pay request is already in progress.", null)
      return
    }

    val request = try {
      PaymentDataRequest.fromJson(requiredPaymentRequestJson())
    } catch (exception: Exception) {
      PendingGooglePayPayment.clear(this)
      emitError("ERR_GOOGLE_PAY_INVALID_REQUEST", "Invalid PaymentDataRequest JSON.", exception)
      return
    }

    paymentInProgress = true

    paymentsClient(activity).loadPaymentData(request)
      .addOnCompleteListener { task ->
        when {
          task.isSuccessful -> {
            PendingGooglePayPayment.clear(this)
            paymentInProgress = false
            handlePaymentData(task.result)
          }

          task.isCanceled -> {
            PendingGooglePayPayment.clear(this)
            paymentInProgress = false
            onCancel(Unit)
          }

          task.exception is ResolvableApiException -> {
            val exception = task.exception as ResolvableApiException
            try {
              activity.startIntentSenderForResult(
                exception.resolution.intentSender,
                GOOGLE_PAY_REQUEST_CODE,
                null,
                0,
                0,
                0,
                null
              )
            } catch (sendException: IntentSender.SendIntentException) {
              PendingGooglePayPayment.clear(this)
              paymentInProgress = false
              emitStatusError(
                "ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA",
                exception.status,
                sendException
              )
            }
          }

          task.exception is ApiException -> {
            PendingGooglePayPayment.clear(this)
            paymentInProgress = false
            emitStatusError(
              "ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA",
              (task.exception as ApiException).status,
              task.exception
            )
          }

          else -> {
            PendingGooglePayPayment.clear(this)
            paymentInProgress = false
            emitError(
              "ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA",
              task.exception?.localizedMessage ?: "Google Pay payment data request failed.",
              task.exception
            )
          }
        }
      }
  }

  fun checkReadiness() {
    val activity = appContext.currentActivity ?: return
    val requestJson = try {
      buildIsReadyToPayRequestJson()
    } catch (exception: Exception) {
      emitError("ERR_GOOGLE_PAY_INVALID_READY_REQUEST", "Invalid IsReadyToPayRequest JSON.", exception)
      return
    } ?: return

    val request = try {
      IsReadyToPayRequest.fromJson(requestJson)
    } catch (exception: JSONException) {
      emitError("ERR_GOOGLE_PAY_INVALID_READY_REQUEST", "Invalid IsReadyToPayRequest JSON.", exception)
      return
    }

    paymentsClient(activity).isReadyToPay(request)
      .addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val isReadyToPay = task.result == true
          payButton.visibility = if (!hideWhenNotReady || isReadyToPay) View.VISIBLE else View.GONE
          onReadyToPayChanged(mapOf("isReadyToPay" to isReadyToPay))
        } else {
          emitError(
            "ERR_GOOGLE_PAY_READY_TO_PAY",
            task.exception?.localizedMessage ?: "Google Pay readiness check failed.",
            task.exception
          )
        }
      }
  }

  internal fun handleActivityResult(resultCode: Int, data: Intent?) {
    paymentInProgress = false

    when (resultCode) {
      Activity.RESULT_OK -> handlePaymentData(data?.let { PaymentData.getFromIntent(it) })
      Activity.RESULT_CANCELED -> onCancel(Unit)
      AutoResolveHelper.RESULT_ERROR -> {
        val status = data?.let { AutoResolveHelper.getStatusFromIntent(it) }
        if (status != null) {
          emitStatusError("ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA", status, null)
        } else {
          emitError("ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA", "Google Pay returned an unknown error.", null)
        }
      }
      else -> emitError(
        "ERR_GOOGLE_PAY_LOAD_PAYMENT_DATA",
        "Google Pay returned unexpected result code $resultCode.",
        null
      )
    }
  }

  private fun configureButton() {
    val allowedPaymentMethods = try {
      extractAllowedPaymentMethodsJson()
    } catch (exception: Exception) {
      emitError("ERR_GOOGLE_PAY_INVALID_BUTTON_OPTIONS", "Invalid Google Pay button options.", exception)
      return
    } ?: return

    try {
      payButton.initialize(
        ButtonOptions.newBuilder()
          .setButtonTheme(parseGooglePayButtonTheme(buttonTheme))
          .setButtonType(parseGooglePayButtonType(buttonType))
          .setCornerRadius((cornerRadius ?: 4.0).toInt())
          .setAllowedPaymentMethods(allowedPaymentMethods)
          .build()
      )
      payButton.isEnabled = !disabled
    } catch (exception: Exception) {
      emitError("ERR_GOOGLE_PAY_BUTTON_INITIALIZE", "Failed to initialize the Google Pay button.", exception)
    }
  }

  private fun handlePaymentData(paymentData: PaymentData?) {
    if (paymentData == null) {
      emitError("ERR_GOOGLE_PAY_EMPTY_PAYMENT_DATA", "Google Pay returned empty payment data.", null)
      return
    }

    val paymentDataJson = paymentData.toJson()
    val paymentMethodData = try {
      JSONObject(paymentDataJson).getJSONObject("paymentMethodData")
    } catch (exception: JSONException) {
      emitError("ERR_GOOGLE_PAY_PAYMENT_DATA_PARSE", "Unable to parse Google Pay payment data.", exception)
      return
    }

    val token = paymentMethodData
      .optJSONObject("tokenizationData")
      ?.optString("token")
      ?.takeIf { it.isNotBlank() }

    if (token == null) {
      emitError("ERR_GOOGLE_PAY_MISSING_TOKEN", "Google Pay payment data did not include a token.", null)
      return
    }

    val info = paymentMethodData.optJSONObject("info")
    val email = JSONObject(paymentDataJson)
      .optString("email")
      .takeIf { it.isNotBlank() }
    val payload = mutableMapOf<String, Any?>(
      "token" to token,
      "paymentData" to paymentDataJson,
      "paymentMethodData" to paymentMethodData.toString(),
      "paymentMethodType" to paymentMethodData.optString("type").takeIf { it.isNotBlank() }
    )

    info?.optString("cardNetwork")?.takeIf { it.isNotBlank() }?.let {
      payload["cardNetwork"] = it
    }
    info?.optString("cardDetails")?.takeIf { it.isNotBlank() }?.let {
      payload["cardDetails"] = it
    }
    email?.let {
      payload["email"] = it
    }

    onTokenReceived(payload)
  }

  private fun paymentsClient(activity: Activity) = Wallet.getPaymentsClient(
    activity,
    Wallet.WalletOptions.Builder()
      .setEnvironment(parseGooglePayEnvironment(environment))
      .build()
  )

  private fun requiredPaymentRequestJson(): String {
    val requestJson = paymentRequestJson?.takeIf { it.isNotBlank() }
    if (requestJson == null) {
      throw JSONException("paymentRequestJson is required.")
    }
    return requestJson
  }

  private fun extractAllowedPaymentMethodsJson(): String? {
    val requestJson = paymentRequestJson?.takeIf { it.isNotBlank() } ?: return null
    return JSONObject(requestJson)
      .getJSONArray("allowedPaymentMethods")
      .toString()
  }

  private fun buildIsReadyToPayRequestJson(): String? {
    isReadyToPayRequestJson?.takeIf { it.isNotBlank() }?.let {
      return it
    }

    val requestJson = paymentRequestJson?.takeIf { it.isNotBlank() } ?: return null
    val paymentRequest = JSONObject(requestJson)
    val allowedPaymentMethods = paymentRequest.getJSONArray("allowedPaymentMethods")
    val readyRequest = JSONObject()
      .put("apiVersion", paymentRequest.optInt("apiVersion", 2))
      .put("apiVersionMinor", paymentRequest.optInt("apiVersionMinor", 0))
      .put("allowedPaymentMethods", JSONArray(allowedPaymentMethods.toString()))

    if (existingPaymentMethodRequired) {
      readyRequest.put("existingPaymentMethodRequired", true)
    }

    return readyRequest.toString()
  }

  private fun emitStatusError(code: String, status: Status, cause: Throwable?) {
    val message = status.statusMessage ?: "Google Pay request failed with status ${status.statusCode}."
    val payload = mutableMapOf<String, Any?>(
      "code" to code,
      "message" to message,
      "statusCode" to status.statusCode,
      "statusMessage" to status.statusMessage
    )
    cause?.localizedMessage?.let {
      payload["nativeMessage"] = it
    }
    onError(payload)
  }

  private fun emitError(code: String, message: String, cause: Throwable?) {
    val payload = mutableMapOf<String, Any?>(
      "code" to code,
      "message" to message
    )
    cause?.localizedMessage?.let {
      payload["nativeMessage"] = it
    }
    onError(payload)
  }
}

internal object PendingGooglePayPayment {
  private var pendingView: WeakReference<ExpoGooglePayView>? = null

  fun begin(view: ExpoGooglePayView): Boolean {
    val currentView = pendingView?.get()
    if (currentView != null && currentView !== view) {
      return false
    }
    pendingView = WeakReference(view)
    return true
  }

  fun clear(view: ExpoGooglePayView) {
    if (pendingView?.get() === view) {
      pendingView = null
    }
  }

  fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode != GOOGLE_PAY_REQUEST_CODE) {
      return false
    }
    val view = pendingView?.get()
    pendingView = null
    view?.handleActivityResult(resultCode, data)
    return true
  }
}

internal fun parseGooglePayEnvironment(environment: String?): Int =
  when (environment?.uppercase(Locale.US)) {
    "PRODUCTION" -> WalletConstants.ENVIRONMENT_PRODUCTION
    else -> WalletConstants.ENVIRONMENT_TEST
  }

private fun parseGooglePayButtonTheme(theme: String?): Int =
  when (theme?.uppercase(Locale.US)) {
    "LIGHT" -> ButtonConstants.ButtonTheme.LIGHT
    else -> ButtonConstants.ButtonTheme.DARK
  }

private fun parseGooglePayButtonType(type: String?): Int =
  when (type?.uppercase(Locale.US)) {
    "BOOK" -> ButtonConstants.ButtonType.BOOK
    "CHECKOUT" -> ButtonConstants.ButtonType.CHECKOUT
    "DONATE" -> ButtonConstants.ButtonType.DONATE
    "ORDER" -> ButtonConstants.ButtonType.ORDER
    "PAY" -> ButtonConstants.ButtonType.PAY
    "PLAIN" -> ButtonConstants.ButtonType.PLAIN
    "SUBSCRIBE" -> ButtonConstants.ButtonType.SUBSCRIBE
    "PIX" -> ButtonConstants.ButtonType.PIX
    "EWALLET" -> ButtonConstants.ButtonType.EWALLET
    else -> ButtonConstants.ButtonType.BUY
  }
