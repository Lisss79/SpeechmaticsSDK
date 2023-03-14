package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.LANGUAGE;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Конфигурация для выравнивания
 */
public class AlignmentConfig {

    private Language language;

    public AlignmentConfig() {
        language = SpeechmaticsSDK.defLanguage;
    }

    public AlignmentConfig(JSONObject json) {
        this();
        try {
            if (json.has(LANGUAGE)) language = Language.getLanguage(json.getString(LANGUAGE));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }


    @NonNull
    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(LANGUAGE, language.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(LANGUAGE, language.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

}
