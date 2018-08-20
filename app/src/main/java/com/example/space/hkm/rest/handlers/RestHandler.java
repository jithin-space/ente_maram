package com.example.space.hkm.rest.handlers;

import com.example.space.hkm.data.Model;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public abstract class RestHandler<T extends Model> extends LoggingJsonHttpResponseHandler {
    public static final String SUCCESS_KEY = "success";

    private T resultObject;

    public RestHandler(T resultObject) {
        this.resultObject = resultObject;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        resultObject.setData(response);
        dataReceived(resultObject);
    }

    // Overridden by consuming class
    public abstract void dataReceived(T responseObject);
}
