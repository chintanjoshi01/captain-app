package com.eresto.captain.utils
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.util.UUID

object FirebaseLogger {

    fun logException(exception: Exception, tag: String = "AppError") {
        val deviceId = getDeviceId()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

        // Log to Crashlytics
        Firebase.crashlytics.setCustomKey("device_id", deviceId)
        Firebase.crashlytics.setCustomKey("device_model", deviceModel)
        Firebase.crashlytics.log("Exception in $tag: ${exception.localizedMessage}")
        Firebase.crashlytics.recordException(exception)

        // Debug Log for development
        Log.e(tag, "Error occurred on Device: $deviceModel (ID: $deviceId)", exception)
    }

    private fun getDeviceId(): String {
        return UUID.randomUUID().toString() // Use a persistent ID logic if needed
    }
}
