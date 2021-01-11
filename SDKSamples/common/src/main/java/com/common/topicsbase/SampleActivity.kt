package com.common.topicsbase

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.common.utils.chat.ChatHolder
import com.common.utils.loginForms.AccountFormController
import com.common.utils.loginForms.AccountFormPresenter
import com.common.utils.loginForms.FormViewModel
import com.common.utils.loginForms.accountUtils.ChatType
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.nanoengine.Account
import com.nanorep.sdkcore.utils.weakRef
import com.sdk.common.R

abstract class SampleActivity  : AppCompatActivity() {

    @ChatType
    abstract val chatType: String
    abstract val containerId: Int
    abstract val extraFormsParams: MutableList<String>

    abstract fun startChat(savedInstanceState: Bundle? = null)

    protected lateinit var chatProvider: ChatHolder
    protected lateinit var chatController: ChatController
    protected lateinit var topicTitle: String

    private lateinit var accountFormController: AccountFormController

    private val formViewModel: FormViewModel by viewModels()

    abstract val onChatLoaded: (fragment: Fragment) -> Unit

    protected open fun getAccount(): Account? = formViewModel.getAccount(baseContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topicTitle = intent.getStringExtra("title") ?: ""

        val formViewModel: FormViewModel by viewModels()

        chatProvider = ChatHolder(baseContext.weakRef(), onChatLoaded)

        accountFormController = AccountFormController(containerId, supportFragmentManager.weakRef())

        formViewModel.chatType = chatType
        formViewModel.extraParams = extraFormsParams

        accountFormController.updateChatType(chatType, extraFormsParams)

        formViewModel.accountData.observe(this, { accountData ->

            chatProvider.accountData = accountData

            supportFragmentManager
                .popBackStack(AccountFormPresenter.LOGIN_FORM, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            startChat(savedInstanceState)

        })
    }

    protected fun hasChatController() = chatProvider.hasChatController()

    override fun onStop() {
        onSampleStop()
        super.onStop()
    }

    protected open fun onSampleStop() {
        if (isFinishing) { chatProvider.destruct() }
    }

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
}