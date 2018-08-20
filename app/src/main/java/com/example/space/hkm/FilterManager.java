package com.example.space.hkm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.atlassian.fugue.Either;
import com.example.space.hkm.data.InstanceInfo;
import com.example.space.hkm.data.Species;
import com.example.space.hkm.data.SpeciesContainer;
import com.example.space.hkm.filters.BaseFilter;
import com.example.space.hkm.filters.BooleanFilter;
import com.example.space.hkm.filters.DefaultFilter;
import com.example.space.hkm.filters.NumericRangeFilter;
import com.example.space.hkm.filters.SpeciesFilter;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.ContainerRestHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FilterManager {
    private RequestGenerator request = new RequestGenerator();

    // All filters loaded from configuration file, set with the latest state
    private LinkedHashMap<String, BaseFilter> allFilters = new LinkedHashMap<>();

    // List of all species received from the API
    private LinkedHashMap<Integer, Species> species = new LinkedHashMap<>();

    private final InstanceInfo instanceInfo;

    public FilterManager(InstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;

        final JSONObject filterDefinitions = instanceInfo.getSearchDefinitions();
        loadSpeciesList();
        loadFilterDefinitions(filterDefinitions);
    }

    public void loadSpeciesList() {
        loadSpeciesList(null);
    }

    public void loadSpeciesList(final Handler.Callback callback) {

        // If species were already lazy loaded, return immediately
        if (species.size() > 0 && callback != null) {
            handleSpeciesCallback(callback, true);
            return;
        }

        request.getAllSpecies(new ContainerRestHandler<SpeciesContainer>(
                new SpeciesContainer()) {

            @Override
            public void dataReceived(SpeciesContainer container) {
                try {
                    species = (LinkedHashMap<Integer, Species>) container
                            .getAll();
                    if (callback != null) {
                        handleSpeciesCallback(callback, true);
                    }
                } catch (JSONException e) {
                    Logger.error("Error in Species retrieval", e);
                }
            }

            @Override
            public void failure(Throwable e, String message) {
                Logger.error(message, e);
                if (callback != null) {
                    handleSpeciesCallback(callback, false);
                }
            }
        });
    }

    private void handleSpeciesCallback(Handler.Callback callback, boolean success) {
        Message resultMessage = new Message();
        Bundle data = new Bundle();
        data.putBoolean("success", success);
        resultMessage.setData(data);
        callback.handleMessage(resultMessage);
    }

    private BaseFilter makeMapFilter(String key, String identifier, String label, String type,
                                     JSONArray choices, String defaultIdentifier) throws Exception {

        if (type.equals("BOOL")) {
            return new BooleanFilter(key, identifier, label);
        }
//        else if (type.equals("RANGE")) {
//            return new NumericRangeFilter(key, identifier, label);
//        } else if (type.equals("DATERANGE")) {
//            return new DateRangeFilter(key, identifier, label);
//        }
        else if (type.equals("SPECIES")) {
            return new SpeciesFilter(key, identifier, label);
        }
//        else if (type.equals("MISSING")) {
//            return new MissingFilter(key, identifier, label);
//        } else if (type.equals("CHOICE")) {
//            return new ChoiceFilter(key, identifier, label, choices);
//        } else if (type.equals("MULTICHOICE")) {
//            return new MultiChoiceFilter(key, identifier, label, choices);
//        } else if (type.equals("STRING")) {
//            return new TextFilter(key, identifier, label);
//        }
        else if (type.equals("DEFAULT")) {
            final JSONObject fieldDef = instanceInfo.getFieldDefinitions().optJSONObject(defaultIdentifier);
            return new DefaultFilter(key, identifier, label, fieldDef);
        } else {
            throw new Exception("Invalid filter type defined in config: " + type);
        }

    }

    private void loadFilterDefinitions(JSONObject filterGroups) {

        loadFilterDef(filterGroups.optJSONArray("standard"), false);
        loadFilterDef(filterGroups.optJSONArray("missing"), true);
    }

    private void loadFilterDef(JSONArray filterDefs, Boolean isMissing) {
        if (filterDefs == null || filterDefs.length() == 0) {
            return;
        }

        String keyPrefix = isMissing ? "m:" : "s:";

        for (int i = 0; i < filterDefs.length(); i++) {
            try {
                JSONObject def = filterDefs.getJSONObject(i);
                String identifier = def.getString("identifier");
                String key = keyPrefix + identifier;
                String label = def.getString("label");
                String type = def.optString("search_type");
                if (isMissing) {
                    type = "MISSING";
                }
                JSONArray choices = def.optJSONArray("choices");
                String defaultIdentifier = def.optString("default_identifier", identifier);

                BaseFilter filter = makeMapFilter(key, identifier, label, type, choices, defaultIdentifier);
                allFilters.put(key, filter);

            } catch (Exception e) {
                Logger.error("Could not create a filter from def # " + i, e);
            }
        }
    }

    /**
     * All species objects indexed by their id.
     */
    public LinkedHashMap<Integer, Species> getSpecies() {
        return species;
    }

    public LinkedHashMap<String, BaseFilter> getFilters() {
        return allFilters;
    }

    /**
     * Returns a comma separated string of active filter names
     */
    public String getActiveFilterDisplay() {
        String display = "", sep = "";
        for (Map.Entry<String, BaseFilter> entry : allFilters.entrySet()) {
            BaseFilter filter = entry.getValue();
            if (filter.isActive()) {
                display += sep + filter.label;
                sep = ", ";
            }
        }
        return display;
    }

    /**
     * Returns the active filters to serialize into a tiler query
     * @return a list of filter objects. Some filters have to be grouped together into JSONArrays,
     *         the others are returned as JSONObjects
     */
    public Collection<Either<JSONObject, JSONArray>> getActiveFilters() {
        final List<Either<JSONObject, JSONArray>> filterObjects = new ArrayList<>();

        // Right now the only default filters are for Alerts search.
        // They need to be grouped into an "OR" group
        final JSONArray defaultFilters = new JSONArray();
        defaultFilters.put("OR");
        for (BaseFilter filter : allFilters.values()) {
            if (filter.isActive()) {
                final JSONObject filterObject = filter.getFilterObject();
                if (filter instanceof DefaultFilter) {
                    defaultFilters.put(filterObject);
                } else {
                    filterObjects.add(Either.left(filterObject));
                }
            }
        }
        if (defaultFilters.length() > 1) {
            filterObjects.add(Either.right(defaultFilters));
        }
        return filterObjects;
    }
}
