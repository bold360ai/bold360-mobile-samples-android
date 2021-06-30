package com.sdk.samples.topics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.common.chatComponents.history.HistoryRepository
import com.common.topicsbase.BoundDataFragment
import com.common.utils.AccessibilityAnnouncer
import com.common.utils.ElementsInterceptor
import com.common.utils.InterceptData
import com.common.utils.live.UploadFileChooser
import com.common.utils.live.onUploads
import com.nanorep.convesationui.structure.SingleLiveData
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.CarouselElement
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.FeedbackElement
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.IncomingElement
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.OutgoingElement
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.QuickOptionsElement
import com.nanorep.convesationui.structure.elements.ChatElement.Companion.UploadElement
import com.nanorep.sdkcore.utils.children
import com.nanorep.sdkcore.utils.px
import com.sdk.samples.R
import com.sdk.samples.databinding.InterceptionTopicBinding

class ElementsInterceptionChat : BotChatHistory() {

    private val announcer = AccessibilityAnnouncer(this)

    private lateinit var interceptor: ElementsInterceptor

    //!- needs to be initiated before Activity's onResume method since it registers to permissions requests
    private val uploadFileChooser = UploadFileChooser(this, 1024 * 1024 * 25)


    private val interceptViewModel: InterceptViewModel
        get() {
            return ViewModelProvider(this).get(InterceptViewModel::class.java)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        interceptViewModel.observeSubmission(this, { ready ->
            if (ready == true) {
                interceptor.apply {
                    interceptionRules = interceptViewModel.interceptions
                    announceRules = interceptViewModel.announcements
                }
                createChat()
            }
        })

        interceptor = ElementsInterceptor(this, announcer)
    }

    override fun onUploadFileRequest() {
        uploadFileChooser.apply {
            onUploadsReady = chatController::onUploads
            open()
        }
    }

    override fun onAccountDataReady() {
        // prevents removal of account form
    }

    override fun startSample(isStateSaved: Boolean) {
        if (!isStateSaved) { //!- check if we're not on saved state recovery to prevent state change and exceptions
            supportFragmentManager.beginTransaction()
                    .add(R.id.basic_chat_view, InterceptionConfig(), topicTitle)
                    .addToBackStack(topicTitle)
                    .commit()
        }
    }

    override fun getChatBuilder(): ChatController.Builder? {
        historyProvider = HistoryRepository(interceptor)
        updateHistoryRepo(account?.getGroupId())

        return super.getChatBuilder()
    }

    override fun onUrlLinkSelected(url: String) {
        // -> announcing link activation on chat
        announcer.announce("u r l  clicked  $url")
    }

    override fun onStop() {
        if (isFinishing) {
            interceptViewModel.observeSubmission(this, null)
        }
        super.onStop()
    }
}


//<editor-fold desc=////////////// Data classes //////////////>

class InterceptViewModel : ViewModel() {
    var interceptions: List<InterceptData> = listOf()

    var announcements: List<InterceptData> = listOf()

    private val submitForm = SingleLiveData<Boolean>()
    fun observeSubmission(owner: LifecycleOwner, observer: Observer<Boolean?>?) {
        submitForm.removeObservers(owner) // only 1 observer allowed
        observer?.let { submitForm.observe(owner, it) }
    }

    fun onSubmitForm(results: Boolean) { // true-startchat false-cancel
        submitForm.value = results
    }
}


class ViewData(type: Int, val resource: Int, isLive: Boolean = false)
    : InterceptData(type, isLive)

//</editor-fold>


//<editor-fold desc=////////////// Interception configuration form //////////////>

class InterceptionConfig : BoundDataFragment<InterceptionTopicBinding>() {

    private val interceptViewModel: InterceptViewModel by activityViewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
            InterceptionTopicBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createDataViews(InterceptElements, binding.interceptGroup, 1)
        createDataViews(AnnouncedElements, binding.announceGroup, 2)

        binding.startChat.setOnClickListener { btn ->
            btn.isClickable = false // prevents multiple activation

            submitData()

            // -> listen to changes on the back stack in order to regain clickable
            //  on the "Start chat" button after disabling it on submission.
            parentFragmentManager.addOnBackStackChangedListener(object : OnBackStackChangedListener {
                override fun onBackStackChanged() {
                    btn.isClickable = true
                    parentFragmentManager.removeOnBackStackChangedListener(this)
                }
            })
        }
    }

    private fun submitData() {
        val onSwitches = arrayListOf<Int>()
        interceptViewModel.interceptions = binding.interceptGroup.children()
                .filter { it is Checkable && it.isChecked }.mapNotNull {
                    when (it) {
                        is CheckBox -> InterceptData(it.tag as Int)
                        is SwitchCompat -> {
                            onSwitches.add(it.tag as Int)
                            null
                        }
                        else -> null
                    }
                }

        // go over switched on switches and set scoped if the matching CheckBoxes were checked:
        onSwitches.forEach { type ->
            interceptViewModel.interceptions.find { it.type == type }?.liveScope = true
        }

        interceptViewModel.announcements = binding.announceGroup.children().mapNotNull { view ->
            (view as? CheckBox)?.takeIf { it.isChecked }?.let { InterceptData(it.tag as Int) }
        }

        // notifies data is ready
        interceptViewModel.onSubmitForm(true)
    }

    private fun createDataViews(dataList: ArrayList<ViewData>, container: ViewGroup, idDelta: Int) {
        context?.let {
            container.removeAllViews()

            dataList.forEach { data ->
                val child = CheckBox(it).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = 10.px
                    }

                    id = data.type * idDelta
                    this.text = context.getString(data.resource)

                    tag = data.type
                }

                container.addView(child)

                if (data.liveScope) {
                    val switch = SwitchCompat(it).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(22.px, (-6).px, 0, 0)
                        }
                        switchPadding = 6.px

                        id = data.type * (idDelta * 2)
                        this.text = context.getString(R.string.live_only)

                        tag = data.type
                    }

                    container.addView(switch)
                }
            }
        }
    }


    companion object {
        val InterceptElements = arrayListOf(ViewData(OutgoingElement, R.string.outgoing_element, true),
                ViewData(IncomingElement, R.string.incoming_element, true),
                ViewData(CarouselElement, R.string.carousel_element),
                ViewData(FeedbackElement, R.string.feedback_element),
                ViewData(QuickOptionsElement, R.string.options_element),
                ViewData(UploadElement, R.string.upload_element))

        val AnnouncedElements = arrayListOf(ViewData(OutgoingElement, R.string.outgoing_element),
                ViewData(IncomingElement, R.string.incoming_element),
                ViewData(QuickOptionsElement, R.string.options_element),
                ViewData(UploadElement, R.string.upload_element))
    }

}

//</editor-fold>