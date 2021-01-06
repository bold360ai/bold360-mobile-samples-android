package com.common.utils.loginForms

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.common.utils.loginForms.SharedDataHandler.Companion.ChatType_key
import com.common.utils.loginForms.accountUtils.ChatType
import com.common.utils.loginForms.accountUtils.ExtraParams.EnableRestore
import com.common.utils.loginForms.accountUtils.ExtraParams.NonSample
import com.nanorep.nanoengine.Account
import java.lang.ref.WeakReference

/**
 * @param restoreRequest is true if the user requested to restore the chat
 * @param restorable is true if the restoration is possible for the account
*/
class RestoreState(val restoreRequest: Boolean = false, var restorable: Boolean = false)

interface AccountListener {
    var onAccountData: ((account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit?)?
}

interface FormController {
    fun updateChatType(
        chatType: String,
        extraParams: List<String>,
        onAccountData: (account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit
    )
}

class AccountFormController(containerRes: Int, wFragmentManager: WeakReference<FragmentManager>):
    FormController {

    private val getFragmentManager: () -> FragmentManager? = { wFragmentManager.get() }

    private val accountFormPresenter = AccountFormPresenter(containerRes)

    override fun updateChatType(
        chatType: String,
        extraParams: List<String>,
        onAccountData: (account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit
    ) {

        accountFormPresenter.onAccountData = onAccountData

        getFragmentManager()?.let { fm ->
            accountFormPresenter.extraParams = extraParams
            accountFormPresenter.presentForm(fm, chatType)
        }
    }
}

interface FormPresenter: AccountListener {
    val containerRes: Int
    val dataController: DataController?

    var extraParams: List<String>

    fun presentForm(fragmentManager: FragmentManager, chatType: String)
}

class AccountFormPresenter(override val containerRes: Int): FormPresenter {

    override val dataController = SharedDataController()

    override lateinit var extraParams: List<String>

    override  var onAccountData: ((account: Account?, restoreState: RestoreState, chatData: Map<String, Any?>?) -> Unit?)?
        set(value) {
            dataController.onAccountData = value
        }
        get() =
            dataController.onAccountData

    override fun presentForm(fragmentManager: FragmentManager, chatType: String) {
        when (chatType) {
            ChatType.None -> presentRestoreForm(fragmentManager)
            else -> presentAccountForm(fragmentManager, chatType)
        }
    }

    private fun presentAccountForm(
        fragmentManager: FragmentManager,
        chatType: String
    ) {
        dataController.chatType = chatType
        startFormTransaction(
            fragmentManager,
            AccountForm.newInstance(dataController, extraParams),
            AccountForm.TAG
        )
    }

    private fun presentRestoreForm(fragmentManager: FragmentManager) {
        val fragment = AccountTypeSelectionForm.newInstance(extraParams.contains(EnableRestore)) { chatType, restoreRequest ->

            dataController.restoreRequest = restoreRequest

            if (chatType != ChatType.None) {
                presentForm(fragmentManager, chatType)
            } else {
                dataController.extraData = mapOf<String, Any>(ChatType_key to ChatType.None)
                dataController.onSubmit(null)
            }
        }

        startFormTransaction(fragmentManager, fragment, AccountTypeSelectionForm.TAG)
    }

    private fun startFormTransaction(fragmentManager: FragmentManager, fragment: Fragment, tag: String) {
        if (!fragmentManager.isStateSaved) {

            val transaction = fragmentManager.beginTransaction()
                .add(containerRes, fragment, tag)

            if (!extraParams.contains(NonSample) || fragmentManager.fragments.isNotEmpty()) {
                transaction.addToBackStack(null)
            }

            transaction.commit()
        }
    }
}