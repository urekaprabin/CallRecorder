package com.callrecorder.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall invoked: $callDetails")

        // Extract phone number from the call handle URI
        val handleUri: Uri? = callDetails.handle
        val phoneNumber = handleUri?.schemeSpecificPart

        if (!phoneNumber.isNullOrEmpty()) {
            Log.d(TAG, "Screened incoming call number: $phoneNumber")
            saveLastIncomingNumber(phoneNumber)
        }

        // Allow the call to proceed normally
        val response = CallResponse.Builder().build()
        respondToCall(callDetails, response)
    }

    private fun saveLastIncomingNumber(number: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_NUMBER, number).apply()
    }

    companion object {
        private const val TAG = "CallScreeningService"
        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_LAST_NUMBER = "last_phone_number"
    }
}
