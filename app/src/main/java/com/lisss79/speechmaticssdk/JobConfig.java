package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.ALIGNMENT_CONFIG;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOB_TYPE;
import static com.lisss79.speechmaticssdk.JsonKeysValues.TRANSCRIPTION_CONFIG;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class JobConfig {

    private JobType jobType;
    private TranscriptionConfig transcriptionConfig;
    private AlignmentConfig alignmentConfig;

    public JobConfig() {
        this.jobType = SpeechmaticsSDK.defJobType;
        this.transcriptionConfig = new TranscriptionConfig();
        this.alignmentConfig = new AlignmentConfig();
    }

    public JobConfig(JSONObject json) {
        this();
        try {
            if (json.has(JOB_TYPE)) jobType = JobType.getJobType(json.getString(JOB_TYPE));
            if (json.has(TRANSCRIPTION_CONFIG)) {
                JSONObject jsonTC = json.getJSONObject(TRANSCRIPTION_CONFIG);
                transcriptionConfig = new TranscriptionConfig(jsonTC);
            }
            if (json.has(ALIGNMENT_CONFIG)) {
                JSONObject jsonAC = json.getJSONObject(ALIGNMENT_CONFIG);
                alignmentConfig = new AlignmentConfig(jsonAC);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public TranscriptionConfig getTranscriptionConfig() {
        return transcriptionConfig;
    }
    public AlignmentConfig getAlignmentConfig() {
        return alignmentConfig;
    }

    public void setTranscriptionConfig(TranscriptionConfig transcriptionConfig) {
        this.transcriptionConfig = transcriptionConfig;
    }

    public void setAlignmentConfig(AlignmentConfig alignmentConfig) {
        this.alignmentConfig = alignmentConfig;
    }

    public static class Builder {
        private final JobConfig jc;
        private final TranscriptionConfig tc;
        private final AlignmentConfig ac;
        public Builder() {
            jc = new JobConfig();
            tc = jc.getTranscriptionConfig();
            ac = jc.getAlignmentConfig();
        }

        public Builder jobType(JobType jt) {
            jc.setJobType(jt);
            return this;
        }
        public Builder language(Language l) {
            tc.setLanguage(l);
            jc.setTranscriptionConfig(tc);
            ac.setLanguage(l);
            jc.setAlignmentConfig(ac);
            return this;
        }
        public Builder diarization(Diarization d) {
            tc.setDiarization(d);
            jc.setTranscriptionConfig(tc);
            return this;
        }
        public Builder operatingPoint(OperatingPoint op) {
            tc.setOperatingPoint(op);
            jc.setTranscriptionConfig(tc);
            return this;
        }
        public JobConfig build() {
            return jc;
        }
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(JOB_TYPE, jobType.getCode());
            json.put(TRANSCRIPTION_CONFIG, transcriptionConfig.toJson());
            json.put(ALIGNMENT_CONFIG, alignmentConfig.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JOB_TYPE, jobType.getCode());
            json.put(TRANSCRIPTION_CONFIG, transcriptionConfig.toJson());
            json.put(ALIGNMENT_CONFIG, alignmentConfig.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

}
