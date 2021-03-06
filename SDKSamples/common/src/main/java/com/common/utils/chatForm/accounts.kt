package com.common.utils.chatForm

import com.integration.async.core.UserInfo
import com.integration.core.userInfo
import com.nanorep.convesationui.async.AsyncAccount
import com.nanorep.convesationui.bold.model.BoldAccount
import com.nanorep.nanoengine.bot.BotAccount
import java.util.UUID

object Accounts {

    private val formalBoldAccount: BoldAccount
        get() = BoldAccount("2300000001700000000:2278936004449775473:sHkdAhpSpMO/cnqzemsYUuf2iFOyPUYV") // Mobile

    val defaultBoldAccount: BoldAccount
        get() = formalBoldAccount

    private val formalBotAccount: BotAccount
        get() = BotAccount(
            "",
            "nanorep",
            "English",
            "" //https://eu1-1.nanorep.com/console/login.html
        )

    val defaultBotAccount: BotAccount
        get() = formalBotAccount

    val defaultAsyncAccount = AsyncAccount(
        "2300000001700000000:2279533687831071375:MlVOftOF/UFUUqPPSbMSDAnQjITxOrQW:gamma",
        "MobileAsyncStagingNew12345"
    ).apply {
        info.userInfo = UserInfo(UUID.randomUUID().toString()).apply {
            firstName = "Android"
            lastName = "Samples"
            email = "android.samples@bold.com"
            phoneNumber = "111-111-1111"
        }
    }
}