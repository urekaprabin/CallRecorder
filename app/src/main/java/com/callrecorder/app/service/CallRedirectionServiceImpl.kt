package com.callrecorder.app.service

import android.content.Context
import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class CallRedirectionServiceImpl : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        val phoneNumber = handle.schemeSpecificPart
        Log.d(TAG, "onPlaceCall: $phoneNumber")

        if (!phoneNumber.isNullOrEmpty()) {
            saveLastOutgoingNumber(phoneNumber)
        }

        // Allow the call to proceed unmodified
        placeCallUnmodified()
    }

    private fun saveLastOutgoingNumber(number: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_OUTGOING, number).apply()
        // Also update last_phone_number so receiver/service knows who we are calling
        prefs.edit().putString("last_phone_number", number).apply()
    }

    companion object {
        private const val TAG = "CallRedirectionService"
        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_LAST_OUTGOING = "last_outgoing_number"
    }
}
