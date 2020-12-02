package com.sdk.samples.common

import android.content.Context
import com.nanorep.convesationui.async.AsyncAccount
import com.nanorep.convesationui.bold.model.BoldAccount
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.bot.BotAccount
import com.sdk.samples.topics.Accounts
import java.lang.ref.WeakReference

interface AccountDataHandler: AccountController {
    var isRestore: Boolean
    var chatType: String?
    fun onSubmit(account: Account)
    fun getAccount(): Map<String, Any?> // Gets the prev account data from the shared properties (according to the ChatType) \n                                                                                      If null it returns the default account
    fun updateAccount(account: Account) // If changed, updates the shared properties to include the updated account details
}

class SharedPrefsDataHandler(val context: WeakReference<Context>, override var chatType: String? = null, override val onAccountReady: (account: Account, isRestore: Boolean) -> Unit): AccountDataHandler {

    override var isRestore: Boolean = false

    override fun onSubmit(account: Account) {
        onAccountReady(account, isRestore)
    }

    private val sharedDataHandler: SharedDataHandler by lazy {
        when (chatType) {
            ChatType.LiveChat -> LiveSharedDataHandler()
            ChatType.AsyncChat -> AsyncSharedDataHandler()
            else -> BotSharedDataHandler()
        }
    }

    override fun getAccount(): Map<String, Any?> {
        return context.get()?.let { sharedDataHandler.getAccountData(it) } ?: getDefaultAccount()
    }

    private fun getDefaultAccount(): Map<String, Any?> {
        return when (chatType) {
                ChatType.LiveChat -> Accounts.defaultBoldAccount.map()
                ChatType.AsyncChat -> Accounts.defaultAsyncAccount.map()
                else -> Accounts.defaultBotAccount.map()
        }
    }

    override fun updateAccount(account: Account) {
        context.get()?.let { sharedDataHandler.saveChatData(it, account) }
    }
}

/**
 * Handles the shared preference interaction to save and retrieve last applied data to forms.
 */
abstract class SharedDataHandler {

    @ChatType
    abstract val chatType: String

    abstract fun getAccountData(context: Context): Map<String, Any?>

    protected fun saveData(context: Context, sharedName: String, data: Map<String, Any?>) {
        val shared = context.getSharedPreferences(sharedName, 0)
        val editor = shared.edit()
        data.forEach { detail ->
            (detail.value as? String)?.run { editor.putString(detail.key, this) }
            (detail.value as? Set<String>)?.run { editor.putStringSet(detail.key, this) }
        }
        editor.apply() //commit()
    }

    abstract fun saveChatData(context: Context, data: Account)

}

class BotSharedDataHandler(): SharedDataHandler() {

    companion object {
        const val SharedName = "ChatDataPref.bot"
        const val Account_key = "accountKey"
        const val Kb_key = "kbKey"
        const val Server_key = "serverKey"
        const val Context_key = "contextKey"
        const val Welcome_key = "welcomeKey"
        const val ApiKey_key = "apiKey"

    }

    override val chatType: String
        get() = ChatType.BotChat

    override fun getAccountData(context: Context): Map<String, Any?> {
        val shared = context.getSharedPreferences(SharedName, 0)
        return mapOf(
            Account_key to shared.getString(Account_key, "nanorep"),
            Kb_key to shared.getString(Kb_key, "English"),
            Server_key to shared.getString(Server_key, ""),
            ApiKey_key to shared.getString(ApiKey_key, "cfeb2b63-785d-48dd-8546-b5444e25a63d"),
            Context_key to shared.getStringSet(Context_key, mutableSetOf())
        )
    }

    override fun saveChatData(context: Context, data: Account) {
        saveData(context, SharedName, (data as? BotAccount)?.map() ?: mapOf())
    }
}

class AsyncSharedDataHandler: SharedDataHandler() {

    companion object {
        const val SharedName = "ChatDataPref.async"
        const val Access_key = "accessKey"
    }

    override val chatType: String
        get() = ChatType.AsyncChat

    override fun getAccountData(context: Context): Map<String, Any?> {
        val shared = context.getSharedPreferences(SharedName, 0)
        return mapOf(Access_key to shared.getString(Access_key, "2307475884:2403340045369405:KCxHNTjbS7qDY3CVmg0Z5jqHIIceg85X:alphawd2"))
    }

    override fun saveChatData(context: Context, data: Account) {
        saveData(context, SharedName, (data as? AsyncAccount)?.map() ?: mapOf())
    }
}

class LiveSharedDataHandler: SharedDataHandler() {

    companion object {
        const val SharedName = "ChatDataPref.bold"
        const val Access_key = "accessKey"
    }

    override val chatType: String
        get() = ChatType.LiveChat

    override fun getAccountData(context: Context): Map<String, Any?> {
        val shared = context.getSharedPreferences(SharedName, 0)
        return mapOf(Access_key to shared.getString(Access_key, "2300000001700000000:2279145895771367548:MGfXyj9naYgPjOZBruFSykZjIRPzT1jl"))
    }

    override fun saveChatData(context: Context, data: Account) {
        saveData(context, SharedName, (data as? BoldAccount)?.map() ?: mapOf())
    }
}
