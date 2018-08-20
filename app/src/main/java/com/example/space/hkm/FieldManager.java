package com.example.space.hkm;

import com.example.space.hkm.data.InstanceInfo;
import com.example.space.hkm.fields.FieldGroup;
import com.example.space.hkm.fields.UDFCollectionFieldGroup;
import com.example.space.hkm.helpers.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FieldManager {

    // All fields loaded from configuration file
    private ArrayList<FieldGroup> allDisplayFields = new ArrayList<>();

    // All field definitions from the api server, these may not be meant for
    // viewing for this application
    private Map<String, JSONObject> baseFields = new HashMap<>();

    // Ordered list of display keys for plot eco benefits
    private String[] ecoKeys;

    public FieldManager(InstanceInfo instance)
            throws Exception {

        setBaseFieldDefinitions(instance.getFieldDefinitions());
        loadFieldDefinitions(instance.getDisplayFieldKeys());
//        loadEcoBenefitKeys(instance.getPlotEcoFields());
    }

    private void loadFieldDefinitions(JSONArray displayData) throws Exception {
        if (this.baseFields.isEmpty()) {
            throw new Exception(
                    "Cannot load field definitions, base fields have not been set");
        }

        try {
            for (int i = 0; i < displayData.length(); i++) {
                JSONObject fieldGroupDef = displayData.getJSONObject(i);
                FieldGroup group = fieldGroupDef.has("collection_udf_keys")
                        ? new UDFCollectionFieldGroup(fieldGroupDef, baseFields)
                        : new FieldGroup(fieldGroupDef, baseFields);
                allDisplayFields.add(group);
            }
        } catch (JSONException e) {
            Logger.error("Unable to load field group", e);
            throw new Exception("Bad field group definition");
        }
    }

    private void setBaseFieldDefinitions(JSONObject fieldDefinitions)
            throws Exception {
        try {
            baseFields.clear();
            Iterator<?> keys = fieldDefinitions.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                baseFields.put(key, fieldDefinitions.getJSONObject(key));
            }

        } catch (JSONException e) {
            Logger.error("Bad Field Definition", e);
            throw new Exception("Incorrectly configured base field list");
        }
    }

    public FieldGroup[] getFieldGroups() {
        return allDisplayFields
                .toArray(new FieldGroup[allDisplayFields.size()]);
    }

}