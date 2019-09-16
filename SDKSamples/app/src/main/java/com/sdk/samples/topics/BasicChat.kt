package com.sdk.samples.topics

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.integration.core.StateEvent
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.controller.ChatEventListener
import com.nanorep.convesationui.structure.controller.ChatLoadResponse
import com.nanorep.convesationui.structure.controller.ChatLoadedListener
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.AccountInfo
import com.nanorep.nanoengine.model.configuration.ConversationSettings
import com.sdk.samples.R
import kotlinx.android.synthetic.main.activity_bot_chat.*

abstract class BasicChat : AppCompatActivity(), ChatEventListener {

    private lateinit var chatController: ChatController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_chat)

        topic_title.text = intent.getStringExtra("title")

    }

    override fun onStart() {
        super.onStart()

        startChat()
    }

    open fun startChat(){
        createChat()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.left_in, R.anim.right_out);
    }

    abstract fun getAccount(): Account

    open protected fun getBuilder() : ChatController.Builder {
        val settings = ConversationSettings()
            .datestamp(true, FriendlyDatestampFormatFactory(this)) // TODO:set as default

        return ChatController.Builder(this)
            .chatEventListener(this)
            .conversationSettings(settings)
    }

    protected fun createChat() {

        chatController = getBuilder().build(
                getAccount(), object : ChatLoadedListener {
                    override fun onComplete(result: ChatLoadResponse) {
                        result.takeIf { it.error == null && it.fragment != null}?.run {
                            supportFragmentManager.beginTransaction()
                                .add(chat_view.id, fragment!!, topic_title.text.toString())
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            )
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d("Chat event", "chat in state: ${stateEvent.state}")
        when (stateEvent.state) {
            StateEvent.ChatWindowDetached -> finish()
        }
    }

    override fun onAccountUpdate(accountInfo: AccountInfo) {
    }

    override fun onPhoneNumberSelected(phoneNumber: String) {
    }

    override fun onUrlLinkSelected(url: String) {
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if(supportFragmentManager.backStackEntryCount == 0){
            finish()
        }
    }

    override fun onStop() {
        if(this::chatController.isInitialized) chatController.terminateChat()

        super.onStop()
    }
}
