package com.sdk.samples.topics

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.format.DateFormat
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringDef
import androidx.core.content.ContextCompat
import com.nanorep.convesationui.structure.UiConfigurations
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.providers.ChatUIProvider
import com.nanorep.convesationui.structure.providers.OutgoingElementUIProvider
import com.nanorep.convesationui.structure.setStyleConfig
import com.nanorep.convesationui.views.StatusIconConfig
import com.nanorep.convesationui.views.adapters.BubbleContentUIAdapter
import com.nanorep.convesationui.views.chatelement.BubbleContentAdapter
import com.nanorep.convesationui.views.chatelement.ViewsLayoutParams
import com.nanorep.nanoengine.chatelement.OutgoingElementModel
import com.nanorep.nanoengine.model.configuration.StyleConfig
import com.nanorep.nanoengine.model.configuration.TimestampStyle
import com.nanorep.sdkcore.model.StatusOk
import com.sdk.samples.R
import kotlinx.android.synthetic.main.bubble_outgoing_demo.view.*
import java.util.*

const val override = "override"
const val configure = "configure"

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@StringDef(override, configure)
annotation class CustomUIOption

open class CustomizedUI : BotChat() {

    override fun getBuilder(): ChatController.Builder {

        val settings = createChatSettings()

        return ChatController.Builder(this)
            .chatEventListener(this)
            .conversationSettings(settings)
            .chatUIProvider(UIProviderFactory.create(this, intent.getStringExtra("type") ?: override))
    }

}

private class UIProviderFactory {

    companion object {

        @JvmStatic
        fun create(context: Context, @CustomUIOption customUIOption: String?): ChatUIProvider {

            return when (customUIOption) {
                configure -> configuring(context)
                else -> overriding(context)
            }
        }

        private fun configuring(context: Context) = ChatUIProvider(context).apply {

            chatElementsUIProvider.outgoingUIProvider.apply {

                configure = { adapter: BubbleContentUIAdapter ->

                        adapter.setTextStyle(StyleConfig(14, Color.RED, Typeface.SANS_SERIF))
                        adapter.setStatusbarAlignment(UiConfigurations.Alignment.AlignStart)
                        adapter.setStatusbarComponentsAlignment(UiConfigurations.StatusbarAlignment.AlignLTR)
                        adapter.setBackground(ColorDrawable(Color.GRAY))
                        adapter
                    }

                customize = { adapter: BubbleContentUIAdapter, element: OutgoingElementModel? ->

                        element?.run {

                            if (elemContent.toLowerCase(Locale.getDefault()).contains("demo")) {
                                adapter.setTextStyle(StyleConfig(color = Color.RED, font = Typeface.SERIF))
                            }

                            if (elemScope.isLive) {
                                adapter.setAvatar(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.agent
                                    )
                                )
                                adapter.setTextStyle(StyleConfig(10, Color.WHITE))
                                adapter.setBackground(ColorDrawable(Color.RED))
                            }

                        }

                        adapter
                    }
            }
        }

        private fun overriding(context: Context) = ChatUIProvider(context).apply {

            chatElementsUIProvider.outgoingUIProvider.apply {

                overrideFactory =

                    object : OutgoingElementUIProvider.OutgoingFactory {

                        override fun createOutgoing(context: Context): BubbleContentAdapter {
                            return OverrideContentAdapter(context)
                        }
                    }

                configure = { adapter: BubbleContentUIAdapter ->

                        adapter.setTextStyle(StyleConfig(14, Color.GRAY, Typeface.SANS_SERIF))
                        adapter.setBackground(ColorDrawable(Color.YELLOW))
                        adapter
                    }
            }
        }
    }
}

/**
 * A demo for custom BubbleContentAdapter implementation
 */
private class OverrideContentAdapter(context: Context): LinearLayout(context), BubbleContentAdapter {

    init {
        View.inflate(getContext(), R.layout.bubble_outgoing_demo, this)

        this.orientation = VERTICAL
        background = ColorDrawable(Color.RED)

        if (layoutParams == null) {
            layoutParams = ViewsLayoutParams.getBubbleDefaultLayoutParams()
        }

    }

    override fun setTime(time: Long) {
        customTimestamp.text = DateFormat.format("MM/dd/yyyy", Date(time)).toString()
    }

    override fun enableTimestampView(enable: Boolean) {
        customTimestamp.visibility = if (enable) View.VISIBLE else View.GONE
    }

    override fun setText(text: Spanned, onLinkPress: ((url: String) -> Unit)?) {
        demo_local_bubble_message_textview.text = text
    }

    override fun setLinkTextColor(color: Int) {
        demo_local_bubble_message_textview.setLinkTextColor(color)
    }

    override fun setTextPadding(left: Int, top: Int, right: Int, bottom: Int) {
        demo_local_bubble_message_textview.setPadding(left, top, right, bottom)
    }

    override fun setBackground(background: Drawable?) {
        demo_local_bubble_message_textview.background = background
    }

    override fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        (layoutParams as? MarginLayoutParams)?.apply {
            this.setMargins(left, top, right, bottom)
        }
    }

    override fun setBubblePadding(left: Int, top: Int, right: Int, bottom: Int) {
        setPadding(left, top, right, bottom)
    }

    override fun setTextStyle(styleConfig: StyleConfig) {
        demo_local_bubble_message_textview.setStyleConfig(styleConfig)
    }

    override fun setStatus(status: Int, statusText: String?) {
        customStatus.text = if (status == StatusOk) "received" else "waiting"
    }

    override fun setDefaultStyle(styleConfig: StyleConfig, timestampStyle: TimestampStyle) {}

    override fun setTextAlignment(hAlignment: Int, vAlignment: Int) {}

    override fun setTextMargins(left: Int, top: Int, right: Int, bottom: Int) {}

    override fun enableStatusbar(enable: Boolean) {}

    override fun enableStatusView(enable: Boolean) {}

    override fun setStatusViewTextStyle(statusStyle: StyleConfig) {}

    override fun setTimestampStyle(timestampStyle: TimestampStyle) {}

    override fun setStatusMargins(left: Int, top: Int, right: Int, bottom: Int) {}

    override fun setStatusIconConfig(statusIcon: StatusIconConfig?) {}

    override fun setStatusIconConfig(styleIdRes: Int) {}

}