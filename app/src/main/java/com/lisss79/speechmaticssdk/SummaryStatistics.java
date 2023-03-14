package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.COUNT;
import static com.lisss79.speechmaticssdk.JsonKeysValues.DURATION_HOURS;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOB_MODE;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOB_TYPE;

import org.json.JSONException;
import org.json.JSONObject;

public class SummaryStatistics {

    private String mode;
    private JobType type;
    private int count;
    private float duration_hrs;

    public SummaryStatistics() {
        mode = "";
        type = JobType.TRANSCRIPTION;
        count = 0;
        duration_hrs = 0;
    }

    public SummaryStatistics(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException {
        if (json.has(JOB_MODE)) mode = json.getString(JOB_MODE);
        if (json.has(JOB_TYPE)) type = JobType.getJobType(json.getString(JOB_TYPE));

        // Получаем число работ и длительность, проверяем на числовой формат
        try {
            if (json.has(COUNT)) count = Integer.parseInt(json.getString(COUNT));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        try {
            if (json.has(DURATION_HOURS)) duration_hrs = Float.parseFloat(json.getString(DURATION_HOURS));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public String getMode() {
        return mode;
    }

    public JobType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public float getDuration_hrs() {
        return duration_hrs;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setDuration_hrs(float duration_hrs) {
        this.duration_hrs = duration_hrs;
    }
}
