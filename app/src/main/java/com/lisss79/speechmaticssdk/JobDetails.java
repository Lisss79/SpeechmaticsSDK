package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.CONFIG;
import static com.lisss79.speechmaticssdk.JsonKeysValues.CREATED_AT;
import static com.lisss79.speechmaticssdk.JsonKeysValues.DATA_NAME;
import static com.lisss79.speechmaticssdk.JsonKeysValues.DURATION;
import static com.lisss79.speechmaticssdk.JsonKeysValues.ID;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOB;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOB_STATUS;
import static com.lisss79.speechmaticssdk.JsonKeysValues.TRANSCRIPTION_CONFIG;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class JobDetails {

    private String createdAt;
    private String dataName;
    private String id;
    private String duration;
    private JobStatus status;
    private JobConfig jobConfig;
    private TranscriptionConfig transcriptionConfig;

    public JobDetails() {
        this.createdAt = "";
        this.dataName = "";
        this.id = "";
        this.duration = "0";
        this.status = JobStatus.NONE;
        this.jobConfig = new JobConfig();
        this.transcriptionConfig = new TranscriptionConfig();
    }

    public JobDetails(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public JobDetails(String response) {
        this();
        JSONObject json;
        try {
            json = new JSONObject(response);
            parseJSON(json);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException {
        if (json.has((JOB))) json = json.getJSONObject((JOB));
        if (json.has(CREATED_AT)) createdAt = json.getString(CREATED_AT);
        if (json.has(DATA_NAME)) dataName = json.getString(DATA_NAME);
        if (json.has(ID)) id = json.getString(ID);
        if (json.has(DURATION)) duration = json.getString(DURATION);
        if (json.has(JOB_STATUS)) status = JobStatus.getJobStatus(json.getString(JOB_STATUS));
        if (json.has(CONFIG)) {
            JSONObject jsonConfig = json.getJSONObject(CONFIG);
            jobConfig = new JobConfig(jsonConfig);
            if (jsonConfig.has(TRANSCRIPTION_CONFIG)) {
                JSONObject jsonTranscriptionConfig = jsonConfig.getJSONObject(TRANSCRIPTION_CONFIG);
                transcriptionConfig = new TranscriptionConfig(jsonTranscriptionConfig);
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "JobDetails{" +
                "createdAt='" + SpeechmaticsSDK.createdToString(createdAt, Locale.getDefault()) + '\'' +
                ", dataName='" + dataName + '\'' +
                ", id='" + id + '\'' +
                ", duration='" + SpeechmaticsSDK.durationToString(duration) + '\'' +
                ", status=" + status.getName() +
                ", language=" + transcriptionConfig.getLanguage().getName() +
                ", diarization=" + transcriptionConfig.getDiarization().getName() +
                ", accuracy=" + transcriptionConfig.getOperatingPoint().getName() +
                '}';
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getDataName() {
        return dataName;
    }

    public String getId() {
        return id;
    }

    public String getDuration() {
        return duration;
    }

    public JobStatus getStatus() {
        return status;
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public TranscriptionConfig getTranscriptionConfig() {
        return transcriptionConfig;
    }
}
