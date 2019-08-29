package com.sdk.samples.topics

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.integration.core.StateEvent
import com.integration.core.annotations.VisitorDataKeys
import com.nanorep.convesationui.bold.model.BoldAccount
import com.sdk.samples.R

class BoldChatAvailability : BoldChat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadAvailabilityCheck()
    }

    private fun loadAvailabilityCheck() {

        val viewModel = ViewModelProvider(this).
            get(CheckAvailabilityViewModel::class.java).apply {
            account = getAccount() as BoldAccount

            onResults = { results ->
                if (results.isAvailable) {

                    val acAccount = getAccount()
                    results.departmentId.takeIf { it > 0 }?.let {
                        account.addExtraData(VisitorDataKeys.Department to results.departmentId)
                    }

                    account.skipPrechat()
                    createChat()
                }
            }
        }

        supportFragmentManager.beginTransaction().add(R.id.chat_view, BoldAvailability(), AvailabilityTag)
            .addToBackStack(AvailabilityTag)
            .commit()
    }

    override fun startChat() {
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d("Chat event", "chat in state: ${stateEvent.state}")
        when (stateEvent.state) {
            StateEvent.ChatWindowDetached -> Log.d(AvailabilityTag, "live chat ended, back to availability checks")
        }
    }

    companion object{
        const val AvailabilityTag = "AvailabilityTag"
    }
}
