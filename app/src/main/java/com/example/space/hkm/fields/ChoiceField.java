package com.example.space.hkm.fields;


import android.app.Activity;
import android.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.widget.Button;

import com.example.space.hkm.Choice;
import com.example.space.hkm.R;
import com.example.space.hkm.data.Model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChoiceField extends ButtonField {
    // Any choices associated with this field, keyed by value with order preserved
    protected final Map<String, Choice> choiceMap = new LinkedHashMap<>();

    // The order of values loaded into selection panel. Used to map index to keys in ChoiceMap
    protected final ArrayList<String> choiceSelectionIndex = new ArrayList<>();
    protected final ArrayList<String> choiceDisplayValues = new ArrayList<>();

    ChoiceField(JSONObject fieldDef) {
        super(fieldDef);
        JSONArray choices = fieldDef.optJSONArray("choices");

        if (choices != null) {
            for (int i = 0; i < choices.length(); i++) {
                JSONObject choiceDef = choices.optJSONObject(i);
                Choice choice = new Choice(choiceDef.optString("display_value"), choiceDef.optString("value"));

                // Dialog choice lists take only an array of strings,
                // and we must later get value by selection index
                choiceMap.put(choice.getValue(), choice);
                choiceSelectionIndex.add(choice.getValue());
                choiceDisplayValues.add(choice.getText());
            }
        }
    }

    /**
     * Format the value with any units, if provided in the definition
     */
    @Override
    public String formatValue(Object value) {
        // If there are choices for this field, display the choice text, not the value
        Choice choice = this.choiceMap.get(value);
        if (choice != null) {
            return choice.getText();
        }
        return null;
    }

    @Override
    protected void setupButton(final AppCompatButton choiceButton, Object value, Model model, Activity activity) {
        String label = formatValueIfPresent(value);
        choiceButton.setText(label);
        choiceButton.setTag(R.id.choice_button_value_tag, value);

        handleChoiceDisplay(choiceButton, this);
    }

    protected void handleChoiceDisplay(final Button choiceButton, final ChoiceField editedField) {
        choiceButton.setOnClickListener(view -> {
            // Determine which item should be selected by default
            Object currentValue = choiceButton.getTag(R.id.choice_button_value_tag);
            int checkedChoiceIndex = -1;

            if (!JSONObject.NULL.equals(currentValue)) {
                checkedChoiceIndex = editedField.choiceSelectionIndex.indexOf(currentValue);
            }

            new AlertDialog.Builder(choiceButton.getContext())
                    .setTitle(editedField.label)
                    .setSingleChoiceItems(editedField.choiceDisplayValues.toArray(new String[0]),
                            checkedChoiceIndex, (dialog, which) -> {
                                String displayText = editedField.choiceDisplayValues.get(which);
                                if (TextUtils.isEmpty(displayText)) {
                                    choiceButton.setText(R.string.unspecified_field_value);
                                } else {
                                    choiceButton.setText(displayText);
                                }
                                choiceButton.setTag(R.id.choice_button_value_tag,
                                        editedField.choiceSelectionIndex.get(which));
                                dialog.dismiss();
                            }
                    )
                    .create().show();
        });
    }
}