package com.capstone.peopleconnect.Helper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.peopleconnect.Client.Fragments.ActivityFragmentClient_BookDetails
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class StripeHelper(
    private val context: Context,
    private val fragment: Fragment
) {
    private lateinit var paymentSheet: PaymentSheet

    init {
        PaymentConfiguration.init(
            context,
            "pk_test_51PF9FWAhzNNxsP4Y16z3rl21cn5T7WtCrwTj2hIIFqXxwbBY3UhjhWtRkpkpQ6FncE9yv6FjHS2SVEEuT0f5zjnj00eAOub2Sx"
        )

        paymentSheet = PaymentSheet(fragment, ::onPaymentSheetResult)
    }

    fun fetchPayment(amount: Double, currency: String, userEmail: String, providerEmail: String, serviceOffered: String) {
        val amountInCents = (amount * 100).toInt()
        Log.d("StripeHelper", "Fetching payment intent for amount: $amountInCents $currency")

        val client = OkHttpClient()
        val url = "https://server-stripe-test.vercel.app/api/create-payment-intent"

        val jsonObject = JSONObject().apply {
            put("amount", amountInCents)
            put("currency", currency)
            put("email", userEmail)
            put("providerEmail", providerEmail)
            put("serviceOffered", serviceOffered)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("StripeHelper", "Network request failed: ${e.message}")
                // Show an error message to the user
                showToast("Network request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("StripeHelper", "Response Data: $responseData")

                    val json = JSONObject(responseData ?: "")
                    val clientSecret = json.getString("clientSecret")
                    val ephemeralKey = json.getString("ephemeralKey")
                    val customerId = json.getString("customerId")

                    // Present payment sheet
                    Handler(Looper.getMainLooper()).post {
                        presentPaymentSheet(clientSecret, ephemeralKey, customerId)
                    }
                } else {
                    Log.e("StripeHelper", "Request failed: ${response.code} - ${response.message}")
                    // Show an error message to the user
                    showToast("Request failed: ${response.code} - ${response.message}")
                }
            }
        })
    }

    private fun presentPaymentSheet(clientSecret: String, ephemeralKey: String, customerId: String) {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                "PeopleConnect",
                PaymentSheet.CustomerConfiguration(
                    customerId,
                    ephemeralKey
                )
            )
        )
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Log.d("StripeHelper", "Payment Canceled")
                // Handle cancellation, you can show a toast or update UI
                showToast("Payment was canceled.")
                (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
            }
            is PaymentSheetResult.Failed -> {
                Log.e("StripeHelper", "Payment Failed: ${paymentSheetResult.error?.message}")
                // Handle failure, you can show a toast or update UI
                showToast("Payment failed: ${paymentSheetResult.error?.message}")
                (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
            }
            is PaymentSheetResult.Completed -> {
                Log.d("StripeHelper", "Payment Successful")
                // Payment was successful
                showToast("Payment was successful.")
                (fragment as? ActivityFragmentClient_BookDetails)?.saveBooking()
            }
        }
    }

    // Helper method to show toast
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
