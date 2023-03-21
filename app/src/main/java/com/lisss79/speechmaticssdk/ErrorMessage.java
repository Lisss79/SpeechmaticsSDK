package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.CODE;
import static com.lisss79.speechmaticssdk.JsonKeysValues.DETAIL;
import static com.lisss79.speechmaticssdk.JsonKeysValues.ERROR;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Класс, содержащий данные об ошибке, полученные от сервера
 */
public class ErrorMessage {
    private int code;
    private String error;
    private String detail;
    private boolean isError;

    public ErrorMessage() {
        code = -1;
        error = "";
        detail = "";
        isError = false;
    }

    public void parseString(String answer) {
        try {
            JSONObject json = new JSONObject(answer);
            if (json.has((CODE))) code = json.getInt((CODE));
            if (json.has(ERROR)) error = json.getString(ERROR);
            if (json.has(DETAIL)) detail = json.getString(DETAIL);
            isError = code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public void parseJSON(JSONObject json) {
        try {
            if (json.has((CODE))) code = json.getInt((CODE));
            if (json.has(ERROR)) error = json.getString(ERROR);
            if (json.has(DETAIL)) detail = json.getString(DETAIL);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }

    public void setCode(int code) {
        this.code = code;
        isError = code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED;
    }

    public void setError(String error) {
        this.error = error;
    }

    @NonNull
    @Override
    public String toString() {
        if(isError)
            return "There is the error. Code= " + code + ", error: " + error + ", detail: " + detail;
        else return "No errors. Response code= " + code;
    }
}
