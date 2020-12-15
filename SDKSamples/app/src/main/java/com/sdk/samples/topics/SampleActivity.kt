package com.sdk.samples.topics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nanorep.convesationui.structure.controller.ChatController
import com.nanorep.nanoengine.Account
import com.sdk.samples.common.AccountProvider
import com.sdk.samples.common.ChatProvider
import com.sdk.samples.common.SamplesViewModel
import com.sdk.samples.common.SingletonSamplesViewModelFactory

abstract class SampleActivity  : AppCompatActivity() {

    private lateinit var singletonSamplesViewModelFactory: SingletonSamplesViewModelFactory

    private lateinit var viewModel: SamplesViewModel

    protected lateinit var chatProvider: ChatProvider
    protected lateinit var accountProvider: AccountProvider

    protected lateinit var chatController: ChatController

    protected open fun getAccount(): Account = accountProvider.account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        singletonSamplesViewModelFactory =  SingletonSamplesViewModelFactory(
            SamplesViewModel.getInstance(application))

        viewModel = ViewModelProvider(this, singletonSamplesViewModelFactory).get(SamplesViewModel::class.java)

        chatProvider = viewModel.chatProvider

        chatController = chatProvider.getChatController()

        accountProvider = viewModel.accountProvider
    }

    protected fun finishIfLast() {
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishIfLast()
    }

}