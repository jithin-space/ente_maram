package com.example.space.hkm.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.example.space.hkm.helpers.Logger;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class InstanceInfo extends Model {

    // Commonly accessed fields are loaded into class
    // fields to avoid having to deal with potential
    // JSONEncoding exceptions through the app
    private int instanceId;
    private String geoRevId;
    private String name;
    private String urlName;

    public class InstanceExtent {
        public final double minLongitude;
        public final double minLatitude;
        public final double maxLongitude;
        public final double maxLatitude;

        public InstanceExtent(double minLng, double minLat, double maxLng, double maxLat) {
            this.minLongitude = minLng;
            this.minLatitude = minLat;
            this.maxLongitude = maxLng;
            this.maxLatitude = maxLat;
        }
    }

    // Default constructor required for RestHandler instantiation
    public InstanceInfo() {
    }

    public InstanceInfo(int instanceId, String geoRevId, String name) {
        this.instanceId = instanceId;
        this.geoRevId = geoRevId;
        this.name = name;
    }

    @Override
    public void setData(JSONObject data) {
        try {
            name = data.getString("name");
            setGeoRevId(data.getString("geoRevHash"));
            urlName = data.getString("url");
            instanceId = data.getInt("id");
            super.setData(data);

        } catch (JSONException ex) {
            Logger.error("Invalid Instance Info Received", ex);
        }
    }

    public String getName() {
        return name;
    }

    public String getGeoRevId() {
        return geoRevId;
    }

    public void setGeoRevId(String geoRevId) {
        this.geoRevId = geoRevId;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getUrlName() {
        return urlName;
    }

    public String getShortDateFormat() {
        return (String) getField("short_date_format");
    }

    public String getLongDateFormat() {
        return (String) getField("date_format");
    }

    public JSONArray getDisplayFieldKeys() {
        return (JSONArray) getField("field_key_groups");
    }

    public JSONObject getFieldDefinitions() {
        return (JSONObject) getField("fields");
    }

    public JSONObject getSearchDefinitions() {
        return (JSONObject) getField("search");
    }

    public double getRadius() throws JSONException { return data.getLong("extent_radius"); }

    public boolean canAddTree() {
        Boolean canAdd = (Boolean) getValueForKey("meta_perms.can_add_tree");
        return canAdd == null ? false : canAdd;
    }

    public boolean canEditTree() {
        Boolean canEdit = (Boolean) getValueForKey("meta_perms.can_edit_tree");
        return canEdit == null ? false : canEdit;
    }

    public boolean canEditTreePhoto() {
        Boolean canEdit = (Boolean) getValueForKey("meta_perms.can_edit_tree_photo");
        return canEdit == null ? false : canEdit;
    }

    public JSONObject getPlotEcoFields() {
        return getEcoFields("plot");
    }

    /**
     * Get eco definitions for a particular model type.
     *
     * @param model ex: 'plot'
     */
    public JSONObject getEcoFields(String model) {
        JSONObject eco = this.data.optJSONObject("eco");
        if (eco != null && eco.optBoolean("supportsEcoBenefits")) {
            JSONArray benefits = eco.optJSONArray("benefits");
            if (benefits != null) {
                for (int i = 0; i < benefits.length(); i++) {
                    JSONObject ecoBen = benefits.optJSONObject(i);
                    if (ecoBen != null && ecoBen.optString("model", "").equalsIgnoreCase(model)) {
                        return ecoBen;
                    }
                }
            }
        }
        return null;
    }

    public double getLat() {
        return getCenter("lat");
    }

    public double getLon() {
        return getCenter("lng");
    }

    public LatLngBounds getExtent() {
        try {
            JSONObject json = data.getJSONObject("extent");

            return new LatLngBounds.Builder()
                    .include(new LatLng(json.getDouble("min_lat"), json.getDouble("min_lng")))
                    .include(new LatLng(json.getDouble("max_lat"), json.getDouble("max_lng")))
                    .build();
        } catch (JSONException e) {
            Logger.error("Invalid Instance extent Received", e);
        }
        return null;
    }

    private double getCenter(String coordinatePart) {
        try {
            JSONObject center = (JSONObject) getField("center");
            return center.getDouble(coordinatePart);
        } catch (JSONException e) {
            Logger.error("Can't get center-part for instance:" + coordinatePart, e);
            return 0;
        }
    }

    public LatLng getStartPos() {
        return new LatLng(getLat(), getLon());
    }

    @Override
    public String toString() {
        return getName();
    }

    public static ArrayList<InstanceInfo> getInstanceInfosFromJSON(JSONArray instances) {
        ArrayList<InstanceInfo> instanceInfos = new ArrayList<>();
        if (instances != null) {
            for (int i = 0; i < instances.length(); i++) {
                InstanceInfo instanceInfo = new InstanceInfo();
                try {
                    instanceInfo.setData(instances.getJSONObject(i));
                    instanceInfos.add(instanceInfo);
                } catch (JSONException e) {
                    Logger.error("Could not load instance info", e);
                }
            }
        }
        return instanceInfos;
    }
}
