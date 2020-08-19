package com.sdk.samples.topics.extra

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.integration.bold.boldchat.core.FormData
import com.integration.bold.boldchat.visitor.api.FormField
import com.integration.bold.boldchat.visitor.api.FormFieldType
import com.nanorep.convesationui.bold.ui.FormListener
import com.nanorep.convesationui.structure.setStyleConfig
import com.nanorep.nanoengine.model.configuration.StyleConfig
import com.nanorep.sdkcore.utils.TextTagHandler
import com.nanorep.sdkcore.utils.forEachChild
import com.nanorep.sdkcore.utils.px
import com.nanorep.sdkcore.utils.weakRef
import com.sdk.samples.R
import kotlinx.android.synthetic.main.custom_live_forms_layout.*
import java.lang.ref.WeakReference


class CustomForm(val data: FormData?, listener: FormListener) : Fragment() {

    private var isSubmitted = false
    private var weakListener: WeakReference<FormListener>? = null

    companion object {
        @JvmStatic fun create(data: FormData, listener: FormListener) : Fragment {
            return CustomForm(data, listener)
        }
    }

    init {
        weakListener = listener.weakRef()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.custom_live_forms_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        fetchForm()

    }

    private fun fetchForm() {
        initFormTypeTitle()
        appendIntroText()
        appendFormFields()
        initSubmitButton()
    }

    private fun initFormTypeTitle() {

        data?.formType?.let { formType ->

            form_type_title.apply {
                setStyleConfig(StyleConfig(20, context.resources.getColor(R.color.colorPrimary), Typeface.DEFAULT_BOLD))
                text = formType
            }
        }
    }

    private fun appendIntroText() {

        data?.getIntroMessage()?.takeIf { it.isNotEmpty() }?.let { introMessage ->

            val introTxt = TextView(context).apply {
                setStyleConfig(StyleConfig(16, Color.DKGRAY, Typeface.DEFAULT_BOLD))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 10.px)
                text = TextTagHandler.getSpannedHtml(introMessage)
            }
            form_fields_container.addView(introTxt)
        }
    }

    private fun appendFormFields() {
        data?.fields?.forEachIndexed { index, fieldData ->

            val fieldView = EditText(context).apply {
                hint = fieldData.label
                id = ViewCompat.generateViewId()
                tag = index
            }
            form_fields_container.addView(fieldView)

            if (fieldData.type == FormFieldType.Select && fieldData.label.contains("Department")) {
                handleDeptView(index, form_fields_container, fieldData, fieldView)
            }
        }
    }

    private fun handleDeptView(index: Int, fieldsContainer: LinearLayout, fieldData: FormField, fieldView: EditText) {

        data?.fields?.takeIf{ it.size - 1  > index }?.let {

            val departmentTitle = TextView(context).apply {
                text = resources.getText(R.string.department_code)
                setTextColor(Color.BLUE)
            }
            fieldsContainer.addView(departmentTitle, index + 1)
        }


        fieldData.options?.run {
            // sets the provided default department as initial value if it's available
            var initialDept = fieldData.defaultOption?.takeIf { it.isDefaultValue && it.isAvailable }?.value ?: ""

            val deptOptionsSB = StringBuilder().append(context?.resources?.getString(R.string.custom_form_departments_title) ?: "")

            forEach {

                deptOptionsSB
                    .appendln(context?.getString(R.string.custom_form_departments_details, it.name ,it.availableLabel, it.value))
                    .appendln()

                if(initialDept.isBlank() && it.isAvailable){
                    initialDept = it.value
                }
            }

            fieldView.text = SpannableStringBuilder(initialDept)

            val deptOptions = TextView(context).apply {
                text = deptOptionsSB
                setTextIsSelectable(true)
            }
            fieldsContainer.addView(deptOptions)
        }

    }

    private fun initSubmitButton() {
        custom_form_submit_button.setOnClickListener {

            form_fields_container?.forEachChild {
                (it as? EditText)?.run {
                    val index = tag as Int
                    data?.fields?.get(index)?.value = this.text.toString()
                }
            }

            isSubmitted = true
            parentFragmentManager.popBackStackImmediate()

            weakListener?.get()?.onComplete(data?.chatForm)
        }
    }

    override fun onStop() {

        if (isRemoving && !isSubmitted) {
            weakListener?.get()?.onCancel(data?.formType)
        }

        super.onStop()
    }
}
