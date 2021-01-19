package com.common.topicsbase

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.common.chatComponents.history.HistoryRepository
import com.common.utils.loginForms.AccountFormController
import com.common.utils.loginForms.AccountFormPresenter
import com.common.utils.loginForms.LoginData
import com.common.utils.loginForms.accountUtils.FormsParams
import com.common.utils.loginForms.dynamicFormPOC.LoginFormViewModel
import com.common.utils.loginForms.dynamicFormPOC.defs.ChatType
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.integration.core.securedInfo
import com.nanorep.convesationui.bold.model.BoldAccount
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.controller.ChatLoadResponse
import com.nanorep.convesationui.structure.controller.ChatLoadedListener
import com.nanorep.nanoengine.Account
import com.nanorep.sdkcore.utils.SystemUtil
import com.nanorep.sdkcore.utils.runMain
import com.nanorep.sdkcore.utils.toast
import com.nanorep.sdkcore.utils.weakRef
import com.sdk.common.R

abstract class SampleActivity  : AppCompatActivity() {

//  <editor-fold desc=">>>>> Chat handling <<<<<" >

    /**
     * Being invoked when the chat fragment had been fetched and ready to be presented
     */
    abstract val onChatLoaded: ((fragment: Fragment) -> Unit)?

    protected lateinit var chatController: ChatController

    /**
     * Returns encrypted info to be added to the Live account (if there is any)
     */
    private fun getSecuredInfo(): String {
        return "some PGP encrypted key string [${SystemUtil.generateTimestamp()}]"
    }

    /**
     * Restores the chat for the current account ( if has ChatController )
     */
    fun restore() {

        if (hasChatController()) {

            chatController.run {
                val continueLast = loginData.account == null

                when {
                    continueLast && hasOpenChats() && isActive -> restoreChat()

                    loginData.restoreState.restorable -> restoreChat(
                        account = prepareAccount(
                            getSecuredInfo()
                        )
                    )

                    else -> {
                        context?.let { toast(it, "The Account is not restorable, a new chat had been created", Toast.LENGTH_SHORT) }
                        startChat(accountInfo = prepareAccount(getSecuredInfo()))
                    }
                }
            }

        } else {
            Log.e(
                "ChatHolder",
                "Failed to restore chat, hasChatController() must be checked first"
            )
        }
    }

    /**
     * Creates the chat chatController and starts the chat
     * @param chatBuilder (optional) injection of a custom ChatController.Builder
     * @return true if the chatController had been created properly, false otherwise
     * When ready the chat fragment would be passed by 'onChatLoaded' invocation
     */
    fun create(chatBuilder: ChatController.Builder? = null): Boolean {

        val chatLoadedListener: ChatLoadedListener = object : ChatLoadedListener {

            override fun onComplete(result: ChatLoadResponse) {
                result.error?.takeIf { baseContext != null }?.run {
                    toast(baseContext!!, "Failed to load chat\nerror:${result.error ?: "failed to get chat fragment"}", Toast.LENGTH_SHORT)
                } ?: runMain {
                    result.fragment?.let {
                        onChatLoaded?.invoke(it)
                    }
                }
            }
        }

        val builder = (chatBuilder ?: baseContext?.let { ChatController.Builder(it) })?.apply {
            historyProvider?.let { chatElementListener(it) }
        }

        prepareAccount(getSecuredInfo()).let { account ->

            builder?.build(account, chatLoadedListener)?.also {
                chatController = it
                return true
            }
        }
        return false
    }

    /**
     * Clears the chat and release its resources
     */
    private fun destructChat() {
        if (hasChatController()) {
            chatController.let {
                it.terminateChat()
                it.destruct()
            }
        }
    }

    lateinit var loginData: LoginData

    private var historyProvider: HistoryRepository? = null


    // History handling

    /**
     * Clears the history and frees its resources
     */
    fun clearHistory() {
        historyProvider?.clear()
    }

    /**
     * Updates the History provider
     */
    fun updateHistoryRepo(historyRepository: HistoryRepository? = null, targetId: String? = null) {
        historyRepository?.let { historyProvider = historyRepository }
        targetId?.let { historyProvider?.targetId = targetId }
    }


    /**
     * @return true if the chat chatController exists and had not been destructed
     */
    fun hasChatController(): Boolean = ::chatController.isInitialized && !chatController.wasDestructed

    private fun prepareAccount(securedInfo: String): Account {
        return getAccount_old().apply {
            if (this is BoldAccount) info.securedInfo = securedInfo
        }
    }

//  </editor-fold>

//  <editor-fold desc=">>>>> Login forms handling <<<<<" >

    /**
     * Called after the LoginData had been updated from the ChatForm
     */
    abstract fun startChat(savedInstanceState: Bundle? = null)

    @ChatType
    open var chatType: String
        set(value) {
            loginFormViewModel.chatType = value
        }
        get() =loginFormViewModel.chatType

    open var formsParams: Int
        set(value) {
            loginFormViewModel.formsParams = value
        }
        get() = loginFormViewModel.formsParams

    abstract val account: Account

    val accountData: JsonObject by lazy {
        loginFormViewModel.getJsonAccount(baseContext)
    }

    open fun validateData(): Boolean = true

    protected val onInvalidAccount: (index: Int, message: String) -> Unit = { index,  message ->  accountFormController.presentError(index, message) }

    open val onChatTypeChanged: ((chatType: String) ->  Unit)?
        get() = null

    protected open val formFields: JsonArray = JsonArray()

    private lateinit var accountFormController: AccountFormController

    private val loginFormViewModel: LoginFormViewModel by viewModels()

    fun addFormsParam(@FormsParams param: Int) {
        loginFormViewModel.formsParams = loginFormViewModel.formsParams or param
    }

    fun getAccount_old(): Account{
        return account
    } // -> would be changed to account property

//  </editor-fold>

    protected lateinit var topicTitle: String
    abstract val containerId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topicTitle = intent.getStringExtra("title").orEmpty()

        val loginFormViewModel: LoginFormViewModel by viewModels()

        accountFormController = AccountFormController(containerId, supportFragmentManager.weakRef())

        loginFormViewModel.formsParams = formsParams
        loginFormViewModel.formFields = formFields

        accountFormController.login(onChatTypeChanged)

        loginFormViewModel.loginData.observe(this, { loginData ->

            this.loginData = loginData

            if ( validateData() ) {

                loginFormViewModel.saveAccount(this)

                supportFragmentManager
                    .popBackStack(
                        AccountFormPresenter.LOGIN_FORM,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )

                startChat(savedInstanceState)

            }

        })
    }

//  <editor-fold desc=">>>>> Base Activity actions <<<<<" >

    override fun onBackPressed() {

        super.onBackPressed()

        supportFragmentManager.executePendingTransactions()

        if (!isFinishing) { finishIfLast() }
    }

    protected fun finishIfLast() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.left_in, R.anim.right_out)
    }

    override fun onStop() {
        onSampleStop()
        super.onStop()
    }

    protected open fun onSampleStop() {
        if (isFinishing) { destructChat() }
    }

//  </editor-fold>

}