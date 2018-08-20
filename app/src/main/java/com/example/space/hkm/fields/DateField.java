package com.example.space.hkm.fields;

import android.app.Activity;
import android.support.v7.widget.AppCompatButton;
import android.widget.Button;

import com.example.space.hkm.R;
import com.example.space.hkm.data.Model;

import org.json.JSONObject;

import static com.example.space.hkm.helpers.DateButtonListener.formatTimestampForDisplay;
import static com.example.space.hkm.helpers.DateButtonListener.getDateButtonListener;

public class DateField extends ButtonField {

    DateField(JSONObject fieldDef) {
        super(fieldDef);
    }

    /**
     * Format the value with any units, if provided in the definition
     */
    @Override
    protected String formatValue(Object value) {
        return formatTimestampForDisplay((String) value);
    }

    @Override
    protected void setupButton(final AppCompatButton choiceButton, Object value, Model model, Activity activity) {
        if (!JSONObject.NULL.equals(value)) {
            final String timestamp = (String) value;
            final String formattedDate = formatTimestampForDisplay(timestamp);
            choiceButton.setText(formattedDate);
            choiceButton.setTag(R.id.choice_button_value_tag, timestamp);
        } else {
            choiceButton.setText(R.string.unspecified_field_value);
        }
        choiceButton.setOnClickListener(getDateButtonListener(activity, R.id.choice_button_value_tag));
    }
}