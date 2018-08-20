package com.example.space.hkm.ui;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.FilterManager;
import com.example.space.hkm.R;
import com.example.space.hkm.adapters.SpeciesAdapter;
import com.example.space.hkm.data.Species;
import com.example.space.hkm.fields.Field;

import java.util.LinkedHashMap;
import java.util.List;

public class SpeciesListDisplay extends FilterableListDisplay<Species> {
    @Override
    protected int getFilterHintTextId() {
        return R.string.filter_species_hint;
    }

    @Override
    protected String getIntentDataKey() {
        return Field.TREE_SPECIES;
    }

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);

        FilterManager search = App.getFilterManager();

        if (search.getSpecies().size() > 0) {
            renderSpeciesList();
        } else {
            search.loadSpeciesList(msg -> {
                if (msg.getData().getBoolean("success")) {
                    renderSpeciesList();
                } else {
                    Toast.makeText(App.getAppInstance(), "Could not get species list",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    private void renderSpeciesList() {
        LinkedHashMap<Integer, Species> list = App.getFilterManager().getSpecies();

        Species[] species = list.values().toArray(new Species[list.size()]);

        // Sectionize by first letter of common name
        LinkedHashMap<CharSequence, List<Species>> speciesSections =
                groupListByKeyFirstLetter(species, Species::getCommonName);

        // Bind the custom adapter to the view
        SpeciesAdapter adapter = new SpeciesAdapter(this, speciesSections);
        Log.d(App.LOG_TAG, list.size() + " species loaded");

        renderList(adapter);
    }
}
