package com.common.utils.chatForm

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.common.topicsbase.BoundDataFragment
import com.common.topicsbase.SampleFormViewModel
import com.common.utils.chatForm.defs.FieldProps
import com.common.utils.chatForm.defs.FieldType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nanorep.sdkcore.utils.children
import com.nanorep.sdkcore.utils.px
import com.sdk.common.R
import com.sdk.common.databinding.ChatFormBinding
import java.lang.AssertionError
import java.util.regex.Pattern

class ChatForm : BoundDataFragment<ChatFormBinding>() {

    private val sampleFormViewModel: SampleFormViewModel by activityViewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): ChatFormBinding =
        ChatFormBinding.inflate(inflater, container, false)

    private lateinit var formFieldsContainer: FormFieldsContainer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        formFieldsContainer = binding.formFieldsContainer

        createForm()

        binding.startChat.apply {
            setOnClickListener {
                Log.d("intercept", "chatform startChat clicked, form visible = ${this@ChatForm.isVisible}")

                collaborateData()
            }
        }
    }

    private fun createForm() {

        sampleFormViewModel.formData.value?.forEach {
            formFieldsContainer.addFormField(it.toObject(true)!!)
        }

    }

    private fun collaborateData() {

        val accountData = JsonObject()

        formFieldsContainer.getFormFields().forEachIndexed { index, view ->

            sampleFormViewModel.getFormField(index)?.run {

                when (view) {

                    is EditText -> getString(FieldProps.Key) to view.text.toString()

                    is RadioGroup -> getString(FieldProps.Key) to view.getSelectedText()

                    is SwitchCompat -> getString(FieldProps.Key) to view.isChecked.toString()

                    is ContextBlock -> {
                        val context = try {view.contextHandler.getContext()}
                        catch (ex:AssertionError){null}
                        context?.let { getString(FieldProps.Key) to Gson().toJson(it).toString()}
                    }

                    else -> null

                }?.let {

                    val isRequired = try {
                        get(FieldProps.Required)?.asBoolean ?: false
                    } catch ( exception : IllegalStateException) {
                        // being thrown by the 'JsonElement' casting
                        Log.w(TAG, exception.message ?: "Unable to parse field")
                        false
                    }

                    val validator = getString(FieldProps.Validator)?.toPattern()
                    if (!isValid(index, it.second, isRequired, validator)) return

                    accountData.addProperty(it.first, it.second)
                }
            }
        }

        sampleFormViewModel.onAccountData(accountData)

    }

    private fun isValid(index: Int, value: String?, required: Boolean, validator: Pattern?): Boolean {

        val presentError: ((message: String) -> Unit) = { message ->
            (formFieldsContainer.getFormFields()[index] as? TextView)?.apply {
                this.requestFocus()
                error = message
            }
        }

        val validatorCheck = {

            validator?.let { // -> If there is a validator, we check that the value passes (empty is valid)

                (value.isNullOrEmpty() || validator.matcher(value).matches()).also {
                    if(!it) presentError(getString(R.string.validation_error))
                }

            } ?: true

        }

        val requiredCheck = {
            (!(required && value.isNullOrEmpty())).also {
                if (!it) presentError(getString(R.string.required_error))
            }
        }

        return validatorCheck() && requiredCheck()
    }

    companion object {

        const val TAG = "ChatForm"

        fun newInstance(): ChatForm {
            return ChatForm()
        }
    }
}

class FormFieldsContainer @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ScrollView(context, attrs, defStyleAttr) {

    private var formFields: LinearLayout

    init {
        setPadding(8.px, 8.px, 8.px, 8.px)

        formFields = LinearLayout(context)
        addView(
            formFields.apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                id = ViewCompat.generateViewId()
            }
        )
    }

    fun getFormFields(): List<View> = formFields.children()

    fun addFormField(fieldData: JsonObject) {

        when (fieldData.getString(FieldProps.Type)) {

            FieldType.Options -> FieldViewFactory.optionsView(
                fieldData.getAsJsonArray("options"),
                context
            )

            FieldType.ContextBlock -> ContextBlock(context).apply {
                initContextBlock(this@FormFieldsContainer)
            }

            FieldType.Title -> FieldViewFactory.titleView(
                fieldData.getString(FieldProps.Value),
                context
            )

            FieldType.TextInput -> FieldViewFactory.inputView(
                fieldData.getString(FieldProps.Value),
                fieldData.getString(FieldProps.Hint),
                context
            )

            FieldType.Switch -> FieldViewFactory.switchView(
                fieldData.getString(FieldProps.Value),
                fieldData.getString(FieldProps.Key),
                context
            )

            else -> null
        }?.let { view -> this.formFields.addView(
            view.apply { id = ViewCompat.generateViewId() })
        }
    }


    private object FieldViewFactory {

        fun titleView(value: String?, context: Context): TextView =
            AppCompatTextView(context).apply {
                text = value ?: ""
                textSize = 22f
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                setPadding(8.px, 8.px, 8.px, 8.px)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
            }

        fun inputView(value: String?, hint: String?, context: Context): EditText =

            AppCompatEditText(context).apply {
                setText(value ?: "")
                this.hint = hint ?: ""
            }

        fun switchView(value: String?, key: String?, context: Context): SwitchCompat =

            SwitchCompat(context).apply {
                isChecked = value == "true"
                textSize = 16f
                text = key ?: ""
            }

        fun optionsView(options: JsonArray, context: Context): RadioGroup {

            return RadioGroup(context).apply {
                options.forEach {
                    addView(
                        AppCompatRadioButton(context).apply {
                            text  = it.toObject()?.getString(FieldProps.Value) ?: ""
                            textSize = 16f
                            id = ViewCompat.generateViewId()
                        }
                    )
                }
                (children.first() as RadioButton).isChecked = true
            }
        }
    }
}
