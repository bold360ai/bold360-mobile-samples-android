package com.sdk.samples.common;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({ChatType.LiveChat, ChatType.BotChat, ChatType.AsyncChat, ChatType.NONE})
public @interface ChatType {
    String LiveChat = "live";
    String AsyncChat = "async";
    String BotChat = "bot";
    String NONE = "none";
}
