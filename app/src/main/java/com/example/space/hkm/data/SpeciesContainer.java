package com.example.space.hkm.data;

import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpeciesContainer extends ModelContainer<Species> {

    @Override
    public Map<Integer, Species> getAll() throws JSONException {
        LinkedHashMap<Integer, Species> speciesList =
                new LinkedHashMap<>(data.length());
        for (int i = 0; i < data.length(); i++) {
            Species species = new Species();
            species.setData(data.getJSONObject(i));
            speciesList.put(species.getId(), species);
        }
        return speciesList;
    }

}
