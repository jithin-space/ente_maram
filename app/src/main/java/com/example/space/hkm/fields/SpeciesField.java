package com.example.space.hkm.fields;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.data.Model;
import com.example.space.hkm.data.Plot;
import com.example.space.hkm.data.Species;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.ui.SpeciesListDisplay;
import com.example.space.hkm.ui.TreeEditDisplay;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;

public class SpeciesField extends ButtonField {

    protected SpeciesField(JSONObject fieldDef) {
        super(fieldDef);
    }

    /*
     * Render a view to display the given model field in view mode
     */
    @Override
    public View renderForDisplay(LayoutInflater layout, Plot plot, Activity activity, ViewGroup parent) throws JSONException {
        View container = layout.inflate(R.layout.plot_field_species_row, parent, false);

        View sciNameRow = container.findViewById(R.id.scientific_name_row);
        TextView sciNamelabel = (TextView) sciNameRow.findViewById(R.id.field_label);
        TextView sciNameValue = (TextView) sciNameRow.findViewById(R.id.field_value);

        // tree.species gets exploded to a double row with sci name and common name
        sciNamelabel.setText(R.string.scientific_name);
        sciNameValue.setText(formatValueIfPresent(plot.getScienticName()));

        View commonNameRow = container.findViewById(R.id.common_name_row);
        TextView commonNameLabel = (TextView) commonNameRow.findViewById(R.id.field_label);
        TextView commonNameValue = (TextView) commonNameRow.findViewById(R.id.field_value);

        commonNameLabel.setText(R.string.common_name);
        commonNameValue.setText(formatValueIfPresent(plot.getCommonName()));

        return container;
    }

    @Override
    protected void setupButton(AppCompatButton button, Object value, Model model, Activity activity) {
        // species could either be truly null, or an actual but empty JSONObject {}
        if (!JSONObject.NULL.equals(value)) {
            // Set the button text to the common and sci name
            String sciName = (String) model.getValueForKey("tree.species.scientific_name");
            String commonName = (String) model.getValueForKey("tree.species.common_name");
            button.setText(commonName + "\n" + sciName);
            Species speciesValue = new Species();
            speciesValue.setData((JSONObject) value);
            this.setValue(speciesValue);
        } else {
            button.setText(R.string.unspecified_field_value);
        }
        button.setOnClickListener(v -> {
            Intent speciesSelector = new Intent(App.getAppInstance(), SpeciesListDisplay.class);
            activity.startActivityForResult(speciesSelector, TreeEditDisplay.FIELD_ACTIVITY_REQUEST_CODE);
        });
    }
    @Override
    public void update(Plot plot) throws Exception {
        // If there is no valueView, this field was not rendered for edit
        if (this.valueView != null) {
            Object currentValue = getEditedValue();

            plot.setValueForKey(key, currentValue);
        }else{
            throw  new Exception("Tree Category is Required");
        }
    }




    @Override
    public void receiveActivityResult(int resultCode, Intent data) {
        CharSequence speciesJSON = data.getCharSequenceExtra(Field.TREE_SPECIES);
        if (!JSONObject.NULL.equals(speciesJSON)) {
            Species species = new Species();
            try {

                species.setData(new JSONObject(speciesJSON.toString()));
                setValue(species);

            } catch (JSONException e) {
                String msg = "Unable to retrieve selected species";
                Logger.error(msg, e);
                Toast.makeText(App.getAppInstance(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Manual setting of the field value from an external client. The only
     * current use case for this is setting the species value on a species
     * selector from the calling activity.
     */
    private void setValue(Species species) {
        if (this.valueView != null) {

            Button speciesButton = (Button) this.valueView;

            if (species != null) {

                speciesButton.setTag(R.id.choice_button_value_tag, species.getData());
                String label = species.getCommonName() + "\n" + species.getScientificName();
                speciesButton.setText(label);
            }
        }
    }
}