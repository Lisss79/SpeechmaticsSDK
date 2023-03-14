package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.SINCE;
import static com.lisss79.speechmaticssdk.JsonKeysValues.UNTIL;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Статистика использования за период
 */
public class UsageStatistics {

    private String since;
    private String until;

    public UsageStatistics() {
        since = "";
        until = "";
    }

    UsageStatistics(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException {
        if (json.has(SINCE)) since = json.getString(SINCE);
        if (json.has(UNTIL)) until = json.getString(UNTIL);
    }
}
