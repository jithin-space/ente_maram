package com.example.space.hkm.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PendingEdit {
    private JSONObject data;

    public PendingEdit(JSONObject definition) {
        data = definition;
    }

    public String getUsername() throws JSONException {
        return data.getString("username");
    }

    public String getValue() throws JSONException {
        return data.getString("value");
    }

    public String getValue(String relatedField) {
        try {
            return data.getJSONObject("related_fields").getString(relatedField);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public int getId() throws JSONException {
        return data.getInt("id");
    }

    public Date getSubmittedTime() throws Exception {
        String when = data.getString("submitted");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'");
        return format.parse(when);
    }

    public void approve() throws Exception {
        // TODO
        throw new Exception("not implemented");
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
