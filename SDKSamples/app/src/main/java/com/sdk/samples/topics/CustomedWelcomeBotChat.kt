package com.sdk.samples.topics

import com.common.utils.chat_form.FormFieldFactory
import com.common.utils.chat_form.defs.DataKeys
import com.common.utils.chat_form.defs.FormType
import com.nanorep.nanoengine.bot.BotAccount

class CustomedWelcomeBotChat : BotChat() {

    override var extraDataFields: () -> List<FormFieldFactory.FormField> = {
        listOf(
            FormFieldFactory.TextInputField(FormType.Account, DataKeys.Welcome, "", "Welcome message id", false)
        )
    }

    companion object{
        const val Customed_WM = "1009689562" //"1009687422"

        const val TestEnv_WM = "871383332"

        // use the following to prevent welcome message to appear on chat
        const val Disable_WM = BotAccount.None
    }
}
