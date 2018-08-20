package com.example.space.hkm.fields;

import android.app.Activity;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.space.hkm.R;
import com.example.space.hkm.data.Model;
import com.example.space.hkm.data.Plot;

import org.json.JSONObject;

public abstract class ButtonField extends Field {
    public ButtonField(JSONObject fieldDef) {
        super(fieldDef);
    }

    @Override
    protected Object getEditedValue() {
        if (this.valueView != null) {
            Object choiceVal = this.valueView.getTag(R.id.choice_button_value_tag);

            if (JSONObject.NULL.equals(choiceVal) || TextUtils.isEmpty(choiceVal.toString())) {
                return null;
            }

            return choiceVal;
        }
        return null;
    }

    /*
     * Render a view to display the given model field in edit mode
     */
    @Override
    public View renderForEdit(LayoutInflater layout, Plot plot, Activity activity, ViewGroup parent) {
        View container = null;

        if (this.canEdit) {
            container = layout.inflate(R.layout.plot_field_edit_button_row, parent, false);
            Object value = plot.getValueForKey(this.key);

            ((TextView) container.findViewById(R.id.field_label)).setText(this.label);
//            Button choiceButton = (Button) container.findViewById(R.id.choice_select);
            AppCompatButton choiceButtonNew = (AppCompatButton) container.findViewById(R.id.choice_select_button);


            this.valueView = choiceButtonNew;

            setupButton(choiceButtonNew, value, plot, activity);
        }

        return container;
    }

    protected abstract void setupButton(AppCompatButton button, Object value, Model model, Activity activity);
}