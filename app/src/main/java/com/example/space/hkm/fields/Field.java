package com.example.space.hkm.fields;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.data.PendingEditDescription;
import com.example.space.hkm.data.Plot;
import com.example.space.hkm.helpers.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public abstract class Field {
    public static final String TREE_SPECIES = "tree.species";
    public static final String TREE_DIAMETER = "tree.diameter";

    public static final String DATE_TYPE = "date";
    public static final String CHOICE_TYPE = "choice";
    public static final String MULTICHOICE_TYPE = "multichoice";

    // This is the view control, either button or EditText, which has the user value
    protected View valueView = null;

    /**
     * The property name from Plot which will contain the data to display or
     * edit. Nested resources are separated by '.' notation
     */
    public final String key;

    /**
     * Label to identify the field on a view
     */
    public final String label;

    /**
     * Does the current user have permission to edit?
     */
    public final boolean canEdit;

    /**
     * Gets the edited value for use when updating
     *
     * @return The edited value - may be of any type
     */
    protected abstract Object getEditedValue();


    /**
     * How to format units
     */
    public final String format;

    public final String infoUrl;

    protected Field(JSONObject fieldDef) {
        key = fieldDef.optString("field_key");
        label = fieldDef.optString("display_name");
        canEdit = fieldDef.optBoolean("can_write");
        format = fieldDef.optString("data_type");

        // NOTE: Not enabled for OTM2 yet
        infoUrl = fieldDef.optString("info_url");
    }

    protected Field(String key, String label) {
        this.key = key;
        this.label = label;
        canEdit = false;
        format = null;
        infoUrl = null;
    }

    public void receiveActivityResult(int resultCode, Intent data) {
        Logger.warning("Received intent data for a field which doesn't start an activity.  Ignoring the intent result.");
    }


    public static Field makeField(JSONObject fieldDef) {
        String format = fieldDef.optString("data_type");
        String key = fieldDef.optString("field_key");

        if (CHOICE_TYPE.equals(format)) {
            return new ChoiceField(fieldDef);
        } else if (MULTICHOICE_TYPE.equals(format)) {
            return new MultiChoiceField(fieldDef);
        } else if (DATE_TYPE.equals(format)) {
            return new DateField(fieldDef);
        } else if (TREE_SPECIES.equals(key)) {
            return new SpeciesField(fieldDef);
        } else if (TREE_DIAMETER.equals(key)) {
//            return new DiameterField(fieldDef);
            return null;
        } else {
            return new TextField(fieldDef);
        }
    }

    /*
   * Render a view to display the given plot field in view mode
   */
    public View renderForDisplay(LayoutInflater layout, Plot plot, Activity activity, ViewGroup parent) throws JSONException {

        // our ui elements
        View container = layout.inflate(R.layout.plot_field_row, parent, false);
        TextView label = (TextView) container.findViewById(R.id.field_label);
        TextView fieldValue = (TextView) container.findViewById(R.id.field_value);
        View infoButton = container.findViewById(R.id.info);
        View pendingButton = container.findViewById(R.id.pending);

        // set the label (simple)
        label.setText(this.label);

        // is this field pending (based on its own notion of pending.)
        Boolean pending = isKeyPending(this.key, plot);

        // Determine the current value of the field and update the ui. (Based on current
        // value or value of simple pending edit
        String value;
        if (!pending) {
            value = formatValueIfPresent(getValueForKey(this.key, plot));
        } else {
            value = plot.getValueForLatestPendingEdit(this.key);
        }
        fieldValue.setText(value);

        // If the key is pending, display the arrow UI, and set up its click handler
        //
        // Note that the semantics of the bindPendingEditClickHandler function take
        // a key into the pending edit array, and an optional related field.
        if (pending) {
            bindPendingEditClickHandler(pendingButton, this.key, plot, activity);
            pendingButton.setVisibility(View.VISIBLE);
        }

        // If the field has a URL attached to it as an info description (IE for pests) display the link.
        if (!TextUtils.isEmpty(this.infoUrl)) {
            infoButton.setVisibility(View.VISIBLE);
            bindInfoButtonClickHandler(infoButton, this.infoUrl, activity);
        }

        return container;
    }

    /**
     * Render a view to display the given plot field in edit mode
     * <p>
     * The Field will not be displayed if this method returns null
     */
    public abstract View renderForEdit(LayoutInflater layout, Plot plot, Activity activity, ViewGroup parent);


    private static boolean isKeyPending(String key, Plot plot) throws JSONException {
        PendingEditDescription pending = plot.getPendingEditForKey(key);
        return pending != null;
    }

    protected String formatValueIfPresent(Object value) {
        // If there is no value, return an unspecified value
        if (JSONObject.NULL.equals(value) || value.equals("")) {
            return App.getAppInstance().getResources().getString(R.string.unspecified_field_value);
        }
        return formatValue(value);
    }

    public void update(Plot plot) throws Exception {
        // If there is no valueView, this field was not rendered for edit
        if (this.valueView != null) {
            Object currentValue = getEditedValue();

            plot.setValueForKey(key, currentValue);
        }
    }

    /**
     * Format the value with any units, if provided in the definition
     */
    protected String formatValue(Object value) {
        return value.toString();
    }

    private static Object getValueForKey(String key, Plot plot) throws JSONException {
        PendingEditDescription pending = plot.getPendingEditForKey(key);
        if (pending != null) {
            return plot.getPendingEditForKey(key).getLatestValue();
        } else {
            return plot.getValueForKey(key);
        }
    }

    /*
     *
     * key : the index into the pending edit array (IE Species) related field:
     * the value to return. (IE Species Name)
     *
     * If related field is null, return the plain value for the field. (Example,
     * when key is DBH, we want the numeric value.)
     */
    private void bindPendingEditClickHandler(View b, final String key, final Plot model,
                                             final Context context) {
//        b.setOnClickListener(v -> {
//            // initialize the intent, and load it with some initial values
//            Intent pendingItemDisplay = new Intent(context, PendingItemDisplay.class);
//            pendingItemDisplay.putExtra("label", label);
//            pendingItemDisplay.putExtra("currentValue", formatValueIfPresent(model.getValueForKey(key)));
//            pendingItemDisplay.putExtra("key", key);
//
//            // Now create an array of pending values, [{id: X, value: "42",
//            // username: "sam"}, ...]
//            PendingEditDescription pendingEditDescription;
//            try {
//                pendingEditDescription = model.getPendingEditForKey(key);
//                List<PendingEdit> pendingEdits = pendingEditDescription.getPendingEdits();
//                JSONArray serializedPendingEdits = new JSONArray();
//                for (PendingEdit pendingEdit : pendingEdits) {
//                    // The value is the plain pending edit's value, or the value of the PE's
//                    // related field. (IE retrieve Species Name instead of a species ID.)
//                    String value = formatValueIfPresent(pendingEdit.getValue());
//
//                    // Continue on loading all of the pending edit data into
//                    // the serializedPendingEdit object
//                    JSONObject serializedPendingEdit = new JSONObject();
//                    serializedPendingEdit.put("id", pendingEdit.getId());
//                    serializedPendingEdit.put("value", value);
//                    serializedPendingEdit.put("username", pendingEdit.getUsername());
//                    try {
//                        serializedPendingEdit.put("date", pendingEdit.getSubmittedTime().toLocaleString());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        serializedPendingEdit.put("date", "");
//                    }
//
//                    // and then append this edit onto the rest of them.
//                    serializedPendingEdits.put(serializedPendingEdit);
//
//                }
//                pendingItemDisplay.putExtra("pending", serializedPendingEdits.toString());
//
//                // And start the target activity
//                Activity a = (Activity) context;
//                a.startActivityForResult(pendingItemDisplay, TreeInfoDisplay.EDIT_REQUEST);
//            } catch (JSONException e1) {
//                Toast.makeText(context, "Sorry, pending edits not available.", Toast.LENGTH_SHORT).show();
//                e1.printStackTrace();
//            }
//        });
    }

    private void bindInfoButtonClickHandler(View infoButton, final String url, final Context context) {
        infoButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Activity a = (Activity) context;
            a.startActivity(browserIntent);
        });
    }



}