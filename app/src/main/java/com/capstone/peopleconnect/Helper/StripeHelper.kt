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
    private var originalAmount: Double = 0.0
    private var commissionAmount: Double = 0.0
    private var totalAmount: Double = 0.0
    private var paymentId: String = ""
    private var paymentMethod: String = ""
    private var paymentDate: String = ""

    init {
        PaymentConfiguration.init(
            context,
            "pk_test_51PF9FWAhzNNxsP4Y16z3rl21cn5T7WtCrwTj2hIIFqXxwbBY3UhjhWtRkpkpQ6FncE9yv6FjHS2SVEEuT0f5zjnj00eAOub2Sx"
        )
        paymentSheet = PaymentSheet(fragment, ::onPaymentSheetResult)
    }

    fun fetchPayment(amount: Double, currency: String, userEmail: String, providerEmail: String, serviceOffered: String) {
        // Store original amount for later use
        this.originalAmount = amount
        
        val client = OkHttpClient()
        val url = "https://server-stripe-test.vercel.app/api/create-payment-intent"

        val jsonObject = JSONObject().apply {
            put("amount", amount)
            put("currency", currency.toLowerCase())
            put("email", userEmail)
            put("providerEmail", providerEmail)
            put("serviceOffered", serviceOffered)
            put("paymentMethod", "card") // Default payment method
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("StripeHelper", "Network request failed: ${e.message}")
                showToast("Network request failed: ${e.message}")
                (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseData = response.body?.string()
                        Log.d("StripeHelper", "Response Data: $responseData")

                        val json = JSONObject(responseData ?: "")
                        
                        // Store all payment details
                        originalAmount = json.getDouble("originalAmount")
                        commissionAmount = json.getDouble("commissionAmount")
                        totalAmount = json.getDouble("totalAmount")
                        paymentId = json.getString("paymentId")
                        paymentMethod = json.getString("paymentMethod")
                        paymentDate = json.getString("paymentDate")

                        // Get payment details
                        val clientSecret = json.getString("clientSecret")
                        val ephemeralKey = json.getString("ephemeralKey")
                        val customerId = json.getString("customerId")
                        
                        // Present payment sheet with updated configuration
                        Handler(Looper.getMainLooper()).post {
                            presentPaymentSheet(
                                clientSecret,
                                ephemeralKey,
                                customerId,
                                paymentId
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("StripeHelper", "Error parsing response: ${e.message}")
                        showToast("Error processing payment details")
                        (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
                    }
                } else {
                    Log.e("StripeHelper", "Request failed: ${response.code} - ${response.message}")
                    showToast("Request failed: ${response.message}")
                    (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
                }
            }
        })
    }

    private fun presentPaymentSheet(
        clientSecret: String,
        ephemeralKey: String,
        customerId: String,
        paymentId: String
    ) {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "PeopleConnect",
            customer = PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = ephemeralKey
            ),
            allowsDelayedPaymentMethods = true
        )

        paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Log.d("StripeHelper", "Payment Canceled")
                showToast("Payment was canceled.")
                (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
            }
            is PaymentSheetResult.Failed -> {
                Log.e("StripeHelper", "Payment Failed: ${paymentSheetResult.error?.message}")
                showToast("Payment failed: ${paymentSheetResult.error?.message}")
                (fragment as? ActivityFragmentClient_BookDetails)?.dismissLoadingDialog()
            }
            is PaymentSheetResult.Completed -> {
                Log.d("StripeHelper", "Payment Successful")
                showToast("Payment successful! Total amount: ₱${"%.2f".format(totalAmount)}")
                
                (fragment as? ActivityFragmentClient_BookDetails)?.let { fragment ->
                    fragment.saveBooking(
                        originalAmount = originalAmount,
                        commissionAmount = commissionAmount,
                        totalAmount = totalAmount,
                        paymentId = paymentId,
                        paymentMethod = paymentMethod,
                        paymentDate = paymentDate
                    )
                }
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun sendReceiptForCompletedBooking(context: Context, paymentId: String) {
            val client = OkHttpClient()
            val url = "https://server-stripe-test.vercel.app/api/receipt"

            val jsonObject = JSONObject().apply {
                put("paymentId", paymentId)
            }

            val requestBody = jsonObject.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("StripeHelper", "Failed to send receipt: ${e.message}")
                    showToast(context, "Failed to send receipt: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val responseData = response.body?.string()
                            val json = JSONObject(responseData ?: "")
                            
                            Log.d("StripeHelper", "Receipt sent successfully: $responseData")
                            showToast(context, "Receipt sent successfully!")
                            
                        } catch (e: Exception) {
                            Log.e("StripeHelper", "Error parsing receipt response: ${e.message}")
                            showToast(context, "Error processing receipt")
                        }
                    } else {
                        Log.e("StripeHelper", "Receipt request failed: ${response.code} - ${response.message}")
                        showToast(context, "Failed to send receipt: ${response.message}")
                    }
                }
            })
        }

        private fun showToast(context: Context, message: String) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
