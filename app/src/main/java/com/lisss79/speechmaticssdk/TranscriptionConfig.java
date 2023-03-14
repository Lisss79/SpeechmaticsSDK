package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.DIARIZATION;
import static com.lisss79.speechmaticssdk.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.JsonKeysValues.OPERATING_POINT;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class TranscriptionConfig {

    private Language language;
    private Diarization diarization;
    private OperatingPoint operatingPoint;

    public TranscriptionConfig() {
        language = SpeechmaticsSDK.defLanguage;
        diarization = SpeechmaticsSDK.defDiarization;
        operatingPoint = SpeechmaticsSDK.defOperatingPoint;
    }

    public TranscriptionConfig(JSONObject json) {
        this();
        try {
            if (json.has(LANGUAGE)) language = Language.getLanguage(json.getString(LANGUAGE));
            if (json.has(DIARIZATION)) diarization = Diarization.getDiarization(json.getString(DIARIZATION));
            if (json.has(OPERATING_POINT)) operatingPoint =
                    OperatingPoint.getOperationPoint(json.getString(OPERATING_POINT));
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

    public Diarization getDiarization() {
        return diarization;
    }

    public void setDiarization(Diarization diarization) {
        this.diarization = diarization;
    }

    public OperatingPoint getOperatingPoint() {
        return operatingPoint;
    }

    public void setOperatingPoint(OperatingPoint operatingPoint) {
        this.operatingPoint = operatingPoint;
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(LANGUAGE, language.getCode());
            json.put(DIARIZATION, diarization.getCode());
            json.put(OPERATING_POINT, operatingPoint.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(LANGUAGE, language.getCode());
            json.put(DIARIZATION, diarization.getCode());
            json.put(OPERATING_POINT, operatingPoint.getCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
