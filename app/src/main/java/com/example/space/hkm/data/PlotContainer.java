package com.example.space.hkm.data;

import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlotContainer extends ModelContainer<Plot> {

    @Override
    public Map<Integer, Plot> getAll() throws JSONException {
        LinkedHashMap<Integer, Plot> plotList =
                new LinkedHashMap<>(data.length());
        for (int i = 0; i < data.length(); i++) {
            Plot plot = new Plot(data.getJSONObject(i));
            plotList.put(plot.getId(), plot);
        }
        return plotList;
    }

    public Plot getFirst() throws JSONException {
        Plot plot = null;
        if (data.length() > 0) {
            plot = new Plot(data.getJSONObject(0));
        }
        return plot;
    }
}
