package com.sdk.samples.common.accountForm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.nanorep.nanoengine.Account
import com.sdk.samples.R
import com.sdk.samples.common.ChatType
import com.sdk.samples.common.DataController


interface AccountFormDelegate {

    /**
     * Controls the data flow from the form and to it
     */
    val dataController: DataController

    /**
     * Takes the fields data from the shared properties
     */
    fun fillFields()

    /**
     * Validates the form data
     * returns Account if the data is valid else null
     */
    fun validateFormData(): Account?

    /**
     * Presents error on a form field
     */
    fun presentError(editText: EditText, message: String?)
}

abstract class AccountForm(override val dataController: DataController) : Fragment(), AccountFormDelegate {

    abstract val formLayoutRes: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(formLayoutRes, container, false)
    }

    private fun validateAndUpdate (): Account? {
        return validateFormData()?.also {
            context?.let { context ->  dataController.updateAccount(context, it) }
        }
    }

    override fun presentError(editText: EditText, message: String?) {
        editText.requestFocus()
        editText.error = message ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        fillFields()

        view.findViewById<Button>(R.id.start_chat).setOnClickListener {
            validateAndUpdate()?.run {
                dataController.onSubmit(this)
            }
        }
    }

    companion object {

        const val TAG = "AccountForm"

        fun newInstance(dataController: DataController, chatType: String): AccountForm {

            dataController.chatType = chatType

            return when (chatType) {
                ChatType.LiveChat -> LiveAccountForm.newInstance(dataController)
                ChatType.AsyncChat -> AsyncAccountForm.newInstance(dataController)
                else -> BotAccountForm.newInstance(dataController)
            }
        }
    }
}