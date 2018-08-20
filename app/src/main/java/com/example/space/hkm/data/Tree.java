package com.example.space.hkm.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Tree extends Model {
    private Plot plot = null;

    public Tree() {
        data = new JSONObject();
    }

    public Tree(Plot parentPlot) {
        data = new JSONObject();
        plot = parentPlot;
    }

    public int getId() throws JSONException {
        return data.getInt("id");
    }

    public void setId(int id) throws JSONException {
        data.put("id", id);
    }

    public String getSpeciesName() throws JSONException {
        return getSpeciesName(false);
    }

    /**
     * Get the current or pending value for species name
     *
     * @param getCurrentOnly , if True return the actual saved value, otherwise return
     *                       pending value if it exists
     * @return
     * @throws JSONException
     */
    public String getSpeciesName(boolean getCurrentOnly) throws JSONException {
        if (getCurrentOnly) {
            return data.getString("species_name");
        }
        Object value = plot.getValueForKey("tree.species_name");
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    public String getSpeciesName(String defaultText) throws JSONException {
        if (data.isNull("species_name")) {
            return defaultText;
        }
        return getSpeciesName();
    }

    public void setSpeciesName(String speciesName) throws JSONException {
        data.put("species_name", speciesName);
    }

    public String getDatePlanted() throws JSONException {
        return data.getString("date_planted");
    }

    public void setDatePlanted(String datePlanted) throws JSONException {
        data.put("date_planted", datePlanted);
    }

    public JSONObject getSpecies() {
        return data.optJSONObject("species");
    }

    public void setSpecies(String species) throws JSONException {
        data.put("species", species);
    }



}