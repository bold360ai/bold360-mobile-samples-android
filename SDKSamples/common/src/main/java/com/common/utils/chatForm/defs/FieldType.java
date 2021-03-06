package com.common.utils.chatForm.defs;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({ FieldType.Title, FieldType.TextInput, FieldType.Switch, FieldType.ContextBlock, FieldType.Options, FieldType.Option })

public @interface FieldType {
    String Title  = "title";
    String TextInput  = "textInput";
    String Switch = "switch";
    String Options = "radioGroup";
    String Option = "radioButton";
    String ContextBlock = "ContextBlock";
}