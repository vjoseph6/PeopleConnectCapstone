package com.capstone.peopleconnect.Helper

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.capstone.peopleconnect.Client.ClientMainActivity
import com.capstone.peopleconnect.R
import com.capstone.peopleconnect.SPrvoider.SProviderMainActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WitAiHandler(private val context: Context) {

    private val client = OkHttpClient()
    private val token = "Bearer RQVZBAYTWBXQHUYUYRNZXM47V6ZSNYLS"

    interface WitAiCallback {
        fun onResponse(bookDay: String, startTime: String, endTime: String, rating: String, serviceType: String, target: String, intent: String)
        fun onError(errorMessage: String)
    }

    fun sendMessageToWit(query: String, callback: WitAiCallback) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://api.wit.ai/message?v=20240922&q=$encodedQuery")
            .addHeader("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WitAiHandler", "Request failed: ${e.message}")
                // Notify error on the main thread
                (context as? ClientMainActivity)?.runOnUiThread {
                    callback.onError("Request failed: ${e.message}")
                }

                (context as? SProviderMainActivity)?.runOnUiThread {
                    callback.onError("Request failed: ${e.message}")
                }

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        parseResponse(responseBody, callback)
                    } ?: Log.e("WitAiHandler", "Response body is null")
                } else {
                    Log.e("WitAiHandler", "Error: ${response.code} ${response.message}")
                    // Notify error on the main thread
                    (context as? ClientMainActivity)?.runOnUiThread {
                        callback.onError("Error: ${response.code} ${response.message}")
                    }
                    (context as? SProviderMainActivity)?.runOnUiThread {
                        callback.onError("Error: ${response.code} ${response.message}")
                    }
                }
            }
        })
    }

    private fun parseResponse(response: String, callback: WitAiCallback) {
        try {
            val jsonResponse = JSONObject(response)
            Log.d("RESPONSE", "$response")
            val entities = jsonResponse.optJSONObject("entities")


            val intentsArray = jsonResponse.optJSONArray("intents")
            var intent = "No intent found"
            var intentConfidence = 0.0

            if (intentsArray != null && intentsArray.length() > 0) {
                val intentObj = intentsArray.optJSONObject(0)
                intent = intentObj?.optString("name") ?: "No intent found"
                intentConfidence = intentObj?.optDouble("confidence") ?: 0.0
                Log.d("Intent", "Intent: $intent, Confidence: $intentConfidence")
            }

            // Extract target and check for multiple targets
            val targetArray = entities?.optJSONArray("target:target")
            if (targetArray != null && targetArray.length() > 1) {
                // Multiple targets found - show error
                Log.e("WitAiHandler", "Multiple targets detected: ${targetArray.length()} targets")
                (context as? ClientMainActivity)?.runOnUiThread {
                    callback.onError("Multiple actions detected. Please specify one action at a time.")
                }
                (context as? SProviderMainActivity)?.runOnUiThread {
                    callback.onError("Multiple actions detected. Please specify one action at a time.")
                }
                return
            }
            
            val target = targetArray?.optJSONObject(0)?.optString("value") ?: "Target not found"
            Log.d("Target", "Target: $target")

            // Extract and display serviceType
            val serviceTypes = mutableListOf<String>()
            val serviceTypeArray = entities?.optJSONArray("serviceType:serviceType")

            if (serviceTypeArray != null) {
                for (i in 0 until serviceTypeArray.length()) {
                    val serviceType = serviceTypeArray.optJSONObject(i)?.optString("value")
                    serviceType?.let { serviceTypes.add(it) }
                }
            }
            Log.d("ServiceType", "Service Type: $serviceTypes")

            val combinedServiceTypes = serviceTypes.joinToString(", ")

            var bookDay: String? = null
            var startTime: String? = null
            var endTime: String? = null

            val datetimeArray = entities?.optJSONArray(context.getString(R.string.dateTime))
            var singleBodyProcessed = false

            if (datetimeArray != null && datetimeArray.length() > 0) {
                for (i in 0 until datetimeArray.length()) {
                    val datetimeObj = datetimeArray.optJSONObject(i)
                    val fromObj = datetimeObj.optJSONObject("from")
                    val toObj = datetimeObj.optJSONObject("to")
                    val grain = datetimeObj.optString("grain")

                    if (fromObj != null && toObj != null) {
                        // This is the single-body case
                        val fromValue = fromObj.optString("value")
                        val toValue = toObj.optString("value")

                        // Log the values for debugging
                        Log.d("SingleBody", "From Value: $fromValue")
                        Log.d("SingleBody", "To Value: $toValue")

                        // Extract and format date and time
                        startTime = fromValue?.let { formatTime(it) }
                        endTime = toValue?.let { formatTime(it, isEndTime = true) }
                        bookDay = fromValue?.let { formatDayOfWeek(it) }

                        // Log formatted values
                        Log.d("SingleBody", "Formatted BookDay: $bookDay")
                        Log.d("SingleBody", "Formatted StartTime: $startTime")
                        Log.d("SingleBody", "Formatted EndTime: $endTime")

                        // Mark single-body processed
                        singleBodyProcessed = true
                        break
                    }
                }

                // If a single-body case was found, search for additional day entities
                if (singleBodyProcessed) {
                    for (i in 0 until datetimeArray.length()) {
                        val datetimeObj = datetimeArray.optJSONObject(i)
                        val grain = datetimeObj.optString("grain")

                        if (grain == "day") {
                            // This entity contains the date
                            val dayValue = datetimeObj.optString("value")?.let { formatDayOfWeek(it) }
                            if (dayValue != null) {
                                bookDay = dayValue
                                Log.d("UpdatedBookDay", "Updated BookDay from Multi-body: $bookDay")
                            }
                        }
                    }
                }
            }

            // Handle default case if no bookDay was found
            if (bookDay.equals("today", ignoreCase = true)) {
                bookDay = "Today"
            }

            // Log final values
            Log.d("FinalValues", "Final BookDay: $bookDay")
            Log.d("FinalValues", "Final StartTime: $startTime")
            Log.d("FinalValues", "Final EndTime: $endTime")

            // Extract and display rating
            val ratingArray = entities?.optJSONArray("rating:rating")
            val rating = ratingArray?.optJSONObject(0)?.optString("value") ?: "Rating not found"
            Log.d("Rating", "Rating: $rating")

            (context as? ClientMainActivity)?.runOnUiThread {
                callback.onResponse(
                    bookDay ?: "",
                    startTime ?: "",
                    endTime ?: "",
                    rating,
                    combinedServiceTypes,
                    target,
                    intent
                )
            }

            (context as? SProviderMainActivity)?.runOnUiThread {
                callback.onResponse(
                    bookDay ?: "",
                    startTime ?: "",
                    endTime ?: "",
                    rating,
                    combinedServiceTypes,
                    target,
                    intent
                )
            }

        } catch (e: Exception) {
            Log.e("WitAiHandler", "Error parsing response", e)
            (context as? ClientMainActivity)?.runOnUiThread {
                callback.onError("Error parsing response: ${e.message}")
            }
            (context as? SProviderMainActivity)?.runOnUiThread {
                callback.onError("Error parsing response: ${e.message}")
            }
        }
    }

    private fun formatDayOfWeek(dateTimeStr: String): String {
        return try {
            // Split the string by 'T' to get the date part
            val datePart = dateTimeStr.split("T")[0]

            // Parse the extracted date string (yyyy-MM-dd) into a LocalDate object
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = LocalDate.parse(datePart, formatter)

            // Get the day of the week in full format (e.g., Thursday)
            val dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

            // Format the date in yyyy-MM-dd format
            val formattedDate = localDate.format(formatter)

            // Return just the formatted date
            formattedDate
        } catch (e: Exception) {
            Log.e("formatDayOfWeek", "Failed to parse date: $dateTimeStr", e)
            "Invalid date"
        }
    }

    // Helper method to format the time (ISO 8601 to 8:00 a.m. format)
    private fun formatTime(isoDateTime: String, isEndTime: Boolean = false): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val date = inputFormat.parse(isoDateTime)

        val calendar = Calendar.getInstance()
        calendar.time = date

        // Adjust timezone
        calendar.add(Calendar.HOUR, -16)

        if (isEndTime) {
            calendar.add(Calendar.SECOND, -1)
        }

        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return outputFormat.format(calendar.time)
    }

}


