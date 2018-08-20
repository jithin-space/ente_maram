package com.example.space.hkm.fields;

import android.view.ViewGroup;

import com.example.space.hkm.data.UDFCollectionDefinition;
import com.example.space.hkm.helpers.JSONHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by space on 7/6/18.
 */

public class UDFCollectionFieldGroup extends FieldGroup {

    private static final int NUM_FIELDS_PER_CLICK = 3;

    private final String title;
    private final String sortKey;
    private final LinkedHashMap<String, UDFCollectionDefinition> udfDefinitions = new LinkedHashMap<>();
    private final LinkedHashMap<String, UDFCollectionDefinition> editableUdfDefinitions = new LinkedHashMap<>();
    private final List<String> fieldKeys;

    private ViewGroup fieldContainer;
//    private List<UDFCollectionValueField> fields;

    public UDFCollectionFieldGroup(JSONObject groupDefinition,
                                   Map<String, JSONObject> fieldDefinitions) throws JSONException {

        title = groupDefinition.optString("header");
        sortKey = groupDefinition.optString("sort_key");
        fieldKeys = JSONHelper.jsonStringArrayToList(groupDefinition.getJSONArray("collection_udf_keys"));

        for (String key : fieldKeys) {
            if (fieldDefinitions.containsKey(key)) {
                final UDFCollectionDefinition udfDef = new UDFCollectionDefinition(fieldDefinitions.get(key));
                udfDefinitions.put(key, udfDef);
                if (udfDef.isWritable()) {
                    editableUdfDefinitions.put(key, udfDef);
                }
            }
        }
    }
}
