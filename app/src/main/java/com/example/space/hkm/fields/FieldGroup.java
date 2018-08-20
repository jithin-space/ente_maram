package com.example.space.hkm.fields;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.space.hkm.R;
import com.example.space.hkm.data.Plot;
import com.example.space.hkm.helpers.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FieldGroup {

    private String title;
    protected Map<String, Field> fields = new LinkedHashMap<>();

    public static enum DisplayMode {VIEW, EDIT}

    protected FieldGroup() {

    }

    public FieldGroup(String title) {
        this.title = title;
    }

    public void update(Plot plot) throws Exception {
        for (Field field : fields.values()) {
            field.update(plot);
        }
    }

    public FieldGroup(JSONObject groupDefinition,
                      Map<String, JSONObject> fieldDefinitions) throws JSONException {

        this.title = groupDefinition.optString("header");
        JSONArray fieldKeys = groupDefinition.getJSONArray("field_keys");

        for (int i = 0; i < fieldKeys.length(); i++) {
            String key = fieldKeys.getString(i);
            addField(key, fieldDefinitions.get(key));
        }
    }

    public void addFields(Map<String, Field> fields) {
        this.fields = fields;
    }

    public void addField(String key, JSONObject fieldDef) {
        if (fieldDef == null) {
            Logger.warning("Missing field definition for display field: " + key);
            return;
        }

        this.fields.put(key, Field.makeField(fieldDef));
    }

    /**
     * Render a field group and its child fields for editing
     */
    public View renderForEdit(LayoutInflater layout, Plot model, Activity activity, ViewGroup parent) {
        return render(layout, model, DisplayMode.EDIT, activity, parent);
    }

    private View render(LayoutInflater layout, Plot model, DisplayMode mode, Activity activity, ViewGroup parent) {

        View container = layout.inflate(R.layout.plot_field_group, parent, false);
        LinearLayout group = (LinearLayout) container.findViewById(R.id.field_group);
        View fieldView;
        int renderedFieldCount = 0;

        ((TextView) group.findViewById(R.id.group_name)).setText(this.title);

        if (this.title != null) {
            for (Field field : fields.values()) {
                try {
                    fieldView = null;
                    switch (mode) {
                        case VIEW:
                            fieldView = field.renderForDisplay(layout, model, activity, group);
                            break;
                        case EDIT:
                            fieldView = field.renderForEdit(layout, model, activity, group);
                            break;
                    }

                    if (fieldView != null) {
                        renderedFieldCount++;
                        group.addView(fieldView);
                    }

                } catch (JSONException e) {
                    Logger.error("Error rendering field '" + field.key + "'", e);
                }
            }
        }
        if (renderedFieldCount > 0) {
            return group;
        } else {
            return null;
        }
    }


    public void receiveActivityResult(int resultCode, Intent data, Activity activity) {
        Set<String> keys = data.getExtras().keySet();
        for (String key : keys) {
            if (fields.containsKey(key)) {
                fields.get(key).receiveActivityResult(resultCode, data);
            }
        }
    }

    /**
     * Render a field group and its child fields for viewing
     */
    public View renderForDisplay(LayoutInflater layout, Plot model, Activity activity, ViewGroup parent) {
        return render(layout, model, DisplayMode.VIEW, activity, parent);
    }



}