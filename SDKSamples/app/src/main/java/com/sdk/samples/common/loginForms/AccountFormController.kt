package com.sdk.samples.common.loginForms

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.nanorep.nanoengine.Account
import com.sdk.samples.R
import com.sdk.samples.common.accountUtils.ChatType.None
import com.sdk.samples.common.loginForms.accountForm.AccountForm
import java.lang.ref.WeakReference

/**
 * @param restoreRequest is true if the user requested to restore the chat
 * @param restorable is true if the restoration is possible for the account
*/
class RestoreState(val restoreRequest: Boolean = false, val restorable: Boolean = false)

interface AccountListener {
    var onAccountData: ((account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit?)?
}

interface FormController {
    fun updateChatType(
        chatType: String,
        extraParams: List<String>?,
        onAccountData: (account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit
    )
}

class AccountFormController(containerRes: Int, wFragmentManager: WeakReference<FragmentManager>):
    FormController {

    private val getFragmentManager: () -> FragmentManager? = { wFragmentManager.get() }

    private val accountFormPresenter = AccountFormPresenter(containerRes)

    override fun updateChatType(
        chatType: String,
        extraParams: List<String>?,
        onAccountData: (account: Account?, restoreState: RestoreState, extraData: Map<String, Any?>?) -> Unit
    ) {

        accountFormPresenter.onAccountData = { account, restoreState, extraData ->
            onAccountData.invoke(account, restoreState, extraData)
        }

        getFragmentManager()?.let { fm ->
            accountFormPresenter.extraParams = extraParams
            chatType.takeIf { it != None }?.let { accountFormPresenter.presentAccountForm(
                fm,
                chatType
            ) } ?: accountFormPresenter.presentRestoreForm(fm)
        }
    }
}

interface FormPresenter: AccountListener {
    val containerRes: Int
    val dataController: DataController?

    var extraParams: List<String>?

    fun presentAccountForm(
        fragmentManager: FragmentManager,
        chatType: String
    )
    fun presentRestoreForm(fragmentManager: FragmentManager)
}

class AccountFormPresenter(override val containerRes: Int): FormPresenter {

    override val dataController = SharedDataController()

    override var extraParams: List<String>? = null

    override  var onAccountData: ((account: Account?, restoreState: RestoreState, chatData: Map<String, Any?>?) -> Unit?)?
        set(value) {
            dataController.onAccountData = value
        }
        get() = dataController.onAccountData

    override fun presentAccountForm(
        fragmentManager: FragmentManager,
        chatType: String
    ) {
        presentForm(
            fragmentManager,
            AccountForm.newInstance(dataController, chatType, extraParams),
            AccountForm.TAG
        )
    }

    override fun presentRestoreForm(fragmentManager: FragmentManager) {
        val fragment = RestoreForm.newInstance { chatType, restoreRequest ->

            dataController.restoreRequest = restoreRequest

            if (chatType != None) {

                presentForm(
                    fragmentManager,
                    AccountForm.newInstance(dataController, chatType, extraParams),
                    AccountForm.TAG
                )

            } else {
                onAccountData?.invoke(null, RestoreState(restoreRequest, false), dataController.extraData)
            }
        }

        presentForm(fragmentManager, fragment, RestoreForm.TAG)
    }

    private fun presentForm(fragmentManager: FragmentManager, fragment: Fragment, tag: String) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.right_in, R.anim.right_out, R.anim.right_in, R.anim.right_out)
            .add(containerRes, fragment, tag)
            .addToBackStack(null)
            .commit()
    }
}