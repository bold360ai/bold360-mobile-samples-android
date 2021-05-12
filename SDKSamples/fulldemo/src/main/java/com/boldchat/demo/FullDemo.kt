package com.boldchat.demo

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.common.chatComponents.NotificationsReceiver
import com.common.chatComponents.customProviders.ContinuityAccountHandler
import com.common.chatComponents.customProviders.CustomTTSAlterProvider
import com.common.chatComponents.handover.CustomHandoverHandler
import com.common.topicsbase.RestorationContinuity
import com.common.utils.chatForm.FormFieldFactory
import com.common.utils.chatForm.defs.ChatType
import com.common.utils.chatForm.defs.DataKeys
import com.common.utils.live.createPickerIntent
import com.common.utils.live.toFileUploadInfo
import com.integration.core.FileUploadInfo
import com.integration.core.InQueueEvent
import com.integration.core.StateEvent
import com.nanorep.convesationui.bold.model.BoldAccount
import com.nanorep.convesationui.structure.FriendlyDatestampFormatFactory
import com.nanorep.convesationui.structure.HandoverHandler
import com.nanorep.convesationui.structure.components.TTSReadAlterProvider
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.convesationui.structure.controller.ChatNotifications
import com.nanorep.nanoengine.Account
import com.nanorep.nanoengine.model.configuration.ChatFeatures
import com.nanorep.nanoengine.model.configuration.ConversationSettings
import com.nanorep.nanoengine.model.configuration.TimestampStyle
import com.nanorep.nanoengine.model.configuration.VoiceSettings
import com.nanorep.nanoengine.model.configuration.VoiceSupport
import com.nanorep.nanoengine.nonbot.EntitiesProvider
import com.nanorep.sdkcore.model.StatementScope
import com.nanorep.sdkcore.model.SystemStatement
import com.nanorep.sdkcore.utils.ErrorException
import com.nanorep.sdkcore.utils.NRError
import com.nanorep.sdkcore.utils.Notifications
import com.nanorep.sdkcore.utils.toast
import com.sdk.common.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

open class FullDemo : RestorationContinuity() {

    override val extraDataFields: (() -> List<FormFieldFactory.FormField>)?
    get() = {
        (super.extraDataFields?.invoke()?.toMutableList() ?: mutableListOf()).apply {
            add(FormFieldFactory.TextInputField( ChatType.Bot, DataKeys.Welcome, "", "Welcome message id", false ))
            add(FormFieldFactory.ContextBlock())
        }
    }

    private var uploadFile: MenuItem? = null

//  <editor-fold desc=">>>>> Chat initialization <<<<<" >

    private val notificationsReceiver = NotificationsReceiver()

    private var accountProvider: ContinuityAccountHandler? = null
    private var handoverHandler: HandoverHandler? = null
    private var ttsAlterProvider: TTSReadAlterProvider? = null

    private var entitiesProvider: EntitiesProvider? = null

    private fun initializeProviders() {
        // Configuring a custom account provider that supports continuity :
        accountProvider = ContinuityAccountHandler(this)

        // Configuring a custom TTS alter provider :
        ttsAlterProvider = CustomTTSAlterProvider()

        // Configuring a custom handover handler :
        handoverHandler = CustomHandoverHandler(baseContext)

        // Uncomment to init the Balance Entities provider handler :
        // entitiesProvider = BalanceEntitiesProvider()

        initInterruptionsReceiver()
    }

    override fun createChatSettings(): ConversationSettings {
        initializeProviders()
        return super.createChatSettings()
            .voiceSettings(VoiceSettings(VoiceSupport.HandsFree))
            .enableMultiRequestsOnLiveAgent(true)
            .datestamp(true, FriendlyDatestampFormatFactory(this))
            .timestampConfig(
                true, TimestampStyle(
                    "dd.MM hh:mm:ss", 10,
                    Color.parseColor("#33aa33"), null
                )
            )
    }

    @ExperimentalCoroutinesApi
    override fun getChatBuilder(): ChatController.Builder? {
        return super.getChatBuilder()?.apply {
            accountProvider?.let { accountProvider(it) }
            handoverHandler?.let { chatHandoverHandler(it) }
            entitiesProvider?.let { entitiesProvider(it) }
            ttsAlterProvider?.let { ttsReadAlterProvider(it) }
        }
    }

    override fun onChatUIDetached() {
        destructMenu?.isVisible = true
        enableMenu(destructMenu, hasChatController())
        super.onChatUIDetached()
    }

    /**
     *   A Broadcast which triggers Interruption to the chat.
     *   This is used to stop the voice recognition/readout during phone actions
     */
    fun initInterruptionsReceiver() {

        LocalBroadcastManager.getInstance(baseContext).registerReceiver(
            object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    Log.d("callAction", "Got broadcast on call action")
                    if (chatController.isActive) {
                        chatController.onChatInterruption()
                    }
                }
            }, IntentFilter("android.CHAT_CALL_ACTION")
        )
    }

    override fun prepareAccount(): Account? {
        return super.prepareAccount()?.apply {
            accountProvider?.prepareAccount(this)
        }
    }

    // Runs on the first creation of the ChatController
    // Afterwards the Chat is being restored/created via the "reloadForms" method
    override fun createChat() {

        // Uncomment to register a Phone call broadcast to trigger onChatInterruption.
        // initInterfaceReceiver()

        // Creates the chat controller
        super.createChat()

        // Registers the app to the wanted chat Notifications
        if ( hasChatController() ) {
            chatController.apply {
                subscribeNotifications(
                    notificationsReceiver,
                    ChatNotifications.PostChatFormSubmissionResults,
                    ChatNotifications.UnavailabilityFormSubmissionResults,
                    Notifications.UploadEnd,
                    Notifications.UploadStart,
                    Notifications.UploadProgress,
                    Notifications.UploadFailed,
                    Notifications.VoiceStopRequest,
                    Notifications.ChatInterruption
                )
            }
        }
    }

//  </editor-fold>

//  <editor-fold desc=">>>>> ChatEventListener implementation <<<<<" >

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d(
            FULL_DEMO_TAG,
            "onChatStateChanged: state " + stateEvent.state + ", scope = " + stateEvent.scope
        )

        when (stateEvent.state) {

            StateEvent.Preparing -> destructMenu?.isVisible = false

            StateEvent.Started -> {
                enableMenu(endMenu, chatController.hasOpenChats())
                if (stateEvent.scope == StatementScope.BoldScope) {
                    enableMenu(uploadFile, chatController.isEnabled(ChatFeatures.FileUpload))
                }
            }

            StateEvent.InQueue -> {
                (stateEvent as? InQueueEvent)?.position?.run {
                    Log.i(FULL_DEMO_TAG, "user is waiting in queue event: user position = $this")
                }
            }

            StateEvent.Unavailable -> lifecycleScope.launch {
                toast(baseContext, stateEvent.state, Toast.LENGTH_SHORT)
            }

            StateEvent.ChatWindowDetached -> onChatUIDetached()

            StateEvent.Ending, StateEvent.Ended -> {
                enableMenu(uploadFile, false)
                if (!chatController.hasOpenChats()) {
                    removeChatFragment()
                }
            }

        }
    }

    override fun onUrlLinkSelected(url: String) {
        // sample code for handling given link
        try {
            Log.d(FULL_DEMO_TAG, ">> got url link selection: [$url]")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (isFileUrl(url)) {
                   /* val uri = FileProvider.getUriForFile(
                       // baseContext, BuildConfig.APPLICATION_ID + ".provider",
                        File(url)
                    )*/

                //    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                } else {
                    data = Uri.parse(url)
                }
            }

            startActivity(intent)

        } catch (e: Exception) {
            Log.w(FULL_DEMO_TAG, ">> Failed to activate link on default app: " + e.message)
            toast(
                baseContext,
                ">> got url: [$url]",
                Toast.LENGTH_SHORT,
                background = ColorDrawable(Color.GRAY)
            )
        }
    }

    private fun isFileUrl(url: String): Boolean {
        return url.startsWith("/")
    }

    override fun onUploadFileRequest() {
        uploadFileRequest()
    }

    //-> previous listener method signature @Override onPhoneNumberNavigation(@NonNull String phoneNumber) {
    override fun onPhoneNumberSelected(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(ChatTag, ">> Failed to activate phone dialer default app: " + e.message)
        }
    }

    /***
     * starts the file upload process. asks for user permissions to browse storage and display the
     * file picker.
     */
    private fun uploadFileRequest() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P ) {
            permissions.plus(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
        getPermissions.launch(permissions)
    }

//  </editor-fold>

//  <editor-fold desc=">>>>> Custom FileUpload implementation <<<<<" >

    private fun handleFileUploads(resultData: Intent) {
        val chosenUploadsTarget = ArrayList<FileUploadInfo>()

        val fileSizeLimit = getUploadSizeLimit(chatController.getScope())

        val fileUri = resultData.data

        fun addChosen(uri: Uri) {
            try {
                uri.toFileUploadInfo(this, fileSizeLimit).let { chosenUploadsTarget.add(it) }

            } catch (ex: ErrorException) {
                chatController.post(
                    SystemStatement(
                        ex.error.description
                            ?: getString(R.string.upload_failure_general)
                    )
                )
            }
        }

        if (fileUri == null) {
            val clipData = resultData.clipData
            if (clipData != null) {
                val itemCount = clipData.itemCount
                for (i in 0 until itemCount) {
                    addChosen(clipData.getItemAt(i).uri)
                }
            }
        } else {
            addChosen(fileUri)
        }

        for (uploadInfo in chosenUploadsTarget) {
            chatController.uploadFile(uploadInfo) { uploadResult ->
                Log.i(FULL_DEMO_TAG, "got Upload results: $uploadResult")

                uploadResult.error?.run {
                    if (NRError.Canceled != reason) {
                        chatController.post(SystemStatement(description ?: reason ?: errorCode))
                    }
                }
            }
        }
    }

    private fun startPickerActivity() {
        createPickerIntent{
            try {
                /*startActivityForResult(
                    Intent.createChooser(intent, "Select files to upload"),
                    FILE_UPLOAD_REQUEST_CODE
                )*/

                fileChooser.launch(it)

            } catch (e: ActivityNotFoundException) {
                toast(baseContext, getString(R.string.FileChooserError), Toast.LENGTH_LONG)
            }
        }
    }

    private fun getUploadSizeLimit(scope: StatementScope): Int = when (scope) {
        StatementScope.BoldScope -> 25 * 1024 * 1024
        else -> -1
    }

//  </editor-fold>

//  <editor-fold desc=">>>>> Menu items customization <<<<<" >

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        super.onCreateOptionsMenu(menu)

        this.uploadFile = menu?.findItem(R.id.upload_file)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        super.onOptionsItemSelected(item)

        when (item.itemId) {
            R.id.upload_file -> {
                uploadFileRequest()
                return true
            }
        }

        return true
    }

//  </editor-fold>

//  <editor-fold desc=">>>>> Lifecycle handling <<<<<" >

    val getPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results:Map<String, Boolean> ->
        val anyFailure = results.any { entry -> !entry.value }
        if(!anyFailure) { // if all permissions were granted
            startPickerActivity()
        }
    }

    val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result:ActivityResult ->
        result.data?.takeIf{result.resultCode == Activity.RESULT_OK}?.run {
            handleFileUploads(this)
        } ?: kotlin.run { Log.w(FULL_DEMO_TAG, "no file was selected to be uploaded") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<TextView>(R.id.topic_title).visibility = View.GONE
    }

    override fun onStop() {
        if (isFinishing && hasChatController()) { chatController.unsubscribeNotifications(notificationsReceiver) }
        super.onStop()
    }

    // Avoids sample finish animation:
    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {}


//  </editor-fold>

    companion object {
        const val FULL_DEMO_TAG = "FullDemo"
        private const val FILE_UPLOAD_REQUEST_CODE = 111
    }
}