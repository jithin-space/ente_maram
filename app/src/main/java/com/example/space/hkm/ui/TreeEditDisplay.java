package com.example.space.hkm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.data.Plot;
import com.example.space.hkm.fields.FieldGroup;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.LoggingJsonHttpResponseHandler;
import com.example.space.hkm.rest.handlers.RestHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cz.msebera.android.httpclient.Header;

import static android.content.ContentValues.TAG;

/**
 * Created by space on 7/5/18.
 */

public class TreeEditDisplay extends TreeDisplay {

    public static final int FIELD_ACTIVITY_REQUEST_CODE = 0;
    protected static final int TREE_MOVE = 2;
    protected static final int PHOTO_USING_CAMERA_RESPONSE = 7;
    protected static final int PHOTO_USING_GALLERY_RESPONSE = 8;
    private ProgressDialog saveDialog = null;
    private Bitmap newTreePhoto;
    private static String outputFilePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        mapFragmentId = R.id.vignette_map_edit_mode;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot_edit_activity);
//        setUpMapIfNeeded();
        initializeEditPage();
//        onMapLoad(map -> {
//            map.setOnMapClickListener(point -> {
//                Intent treeMoveIntent = new Intent(TreeEditDisplay.this, TreeMove.class);
//                treeMoveIntent.putExtra("plot", plot.getData().toString());
//                startActivityForResult(treeMoveIntent, TREE_MOVE);
//            });
//        });



        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());


    }

    private void changeFAB(){
        if(this.newTreePhoto !=null){
            ((FloatingActionButton)findViewById(R.id.edit_tree_picture)).setImageResource(R.drawable.ic_inserted_photo);
            ((FloatingActionButton)findViewById(R.id.edit_tree_picture)).setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.md_green_800)));
        }
    }




    private void initializeEditPage() {

        if (plot == null) {
            finish();
        }

        LinearLayout fieldList = (LinearLayout) findViewById(R.id.field_list);
        LayoutInflater layout = this.getLayoutInflater();

        // Add all the fields to the display for edit mode
        for (FieldGroup group : App.getFieldManager().getFieldGroups()) {
            View fieldGroup = group.renderForEdit(layout, plot, TreeEditDisplay.this, fieldList);
            if (fieldGroup != null) {
                fieldList.addView(fieldGroup);
            }
        }

//        setupDeleteButtons(layout, fieldList);
    }


    public void save(View view){
        Log.d(TAG, "save:saving ");
        save();
    }

    public void cancel(View view){
        finish();
    }

    public void addTreePhoto(View view){
        changeTreePhoto();
    }

    public void changeTreePhoto() {
        Log.d("PHOTO", "changePhoto");

        if (!App.getCurrentInstance().canEditTreePhoto()) {
            Toast.makeText(getApplicationContext(), getString(R.string.perms_add_tree_photo_fail), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setView(R.layout.image_selector_layout);
//
//        builder.setNegativeButton(R.string.use_camera, (dialog, id) -> {
//            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            File outputFile = PhotoActivity.createImageFile();
//            outputFilePath = outputFile.getAbsolutePath();
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
//            startActivityForResult(intent, PHOTO_USING_CAMERA_RESPONSE);
//        }).setIcon(R.drawable.ic_add_photo);
//        builder.setPositiveButton(R.string.use_gallery, (dialog, id) -> {
//            Intent intent = new Intent(Intent.ACTION_PICK,
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            startActivityForResult(intent, PHOTO_USING_GALLERY_RESPONSE);
//        });




        builder.setNegativeButton("Cancel",null);
        AlertDialog alert = builder.create();
        alert.show();

        RadioGroup radioGroup = (RadioGroup) alert.findViewById(R.id.radioGroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected
                if (checkedId == R.id.radioButton_camera) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File outputFile = PhotoActivity.createImageFile();
                    outputFilePath = outputFile.getAbsolutePath();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
                    startActivityForResult(intent, PHOTO_USING_CAMERA_RESPONSE);
                    alert.dismiss();
                }
                else if (checkedId == R.id.radioButton_gallery) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, PHOTO_USING_GALLERY_RESPONSE);
                    alert.dismiss();

                }
            }
        });

        changeFAB();
    }

    private void save() {
        saveDialog = ProgressDialog.show(this, "", "Saving...", true);

        try {

            for (FieldGroup group : App.getFieldManager().getFieldGroups()) {
                group.update(plot);
            }

            RequestGenerator rg = new RequestGenerator();

            RestHandler<Plot> responseHandler = new RestHandler<Plot>(new Plot()) {
                @Override
                public void dataReceived(Plot updatedPlot) {
                    // Tree was updated, check if a photo needs to be also added
                    savePhotoForPlot(updatedPlot);

                    Log.d(TAG, "dataReceived: ");
                }

                @Override
                public void failure(Throwable e, String responseBody) {
                    Logger.warning("Failure saving tree", e);
                    handleSaveFailure(e);
                }
            };

            if (addMode()) {
                rg.addPlot(plot, responseHandler);
            } else {
                if (App.getCurrentInstance().canEditTree()) {
                    rg.updatePlot(plot, responseHandler);
                } else {
                    savePhotoForPlot(plot);
                }
            }
        } catch (Exception e) {
            handleSaveFailure(e);
        }
    }

    private void savePhotoForPlot(final Plot updatedPlot) {
        if (this.newTreePhoto == null) {
            doFinish(updatedPlot, saveDialog);
            return;
        }

        if (!App.getCurrentInstance().canEditTreePhoto()) {
            handlePhotoSaveFailure(null);
            return;
        }

        RequestGenerator rc = new RequestGenerator();
        try {
            rc.addTreePhoto(updatedPlot, this.newTreePhoto, new LoggingJsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        if (response.has("image")) {
                            updatedPlot.assignNewTreePhoto(response);

                            safeDismiss(saveDialog);
                            doFinish(updatedPlot, saveDialog);

                        } else {
                            handlePhotoSaveFailure(null);
                        }
                    } catch (JSONException e) {
                        handlePhotoSaveFailure(e);
                    }
                }

                @Override
                public void failure(Throwable e, String errorResponse) {
                    handlePhotoSaveFailure(e);
                }
            });
        } catch (JSONException e) {
            handlePhotoSaveFailure(e);
        }
    }



    private void handlePhotoSaveFailure (Throwable e) {
        String msg = getString(R.string.save_tree_photo_failure);
        Logger.error(msg, e);
        safeDismiss(saveDialog);
        Toast.makeText(App.getAppInstance(), msg, Toast.LENGTH_SHORT).show();
    }

    private void doFinish(Plot updatedPlot, ProgressDialog saveDialog) {
        safeDismiss(saveDialog);
        setResultOk(updatedPlot);

        // Updating may have changed the georev
        App.getCurrentInstance().setGeoRevId(updatedPlot.getUpdatedGeoRev());

        finish();
    }

    /**
     * Set the result code to OK and set the updated plot as an intent extra
     *
     * @param updatedPlot
     */
    private void setResultOk(Plot updatedPlot) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("plot", updatedPlot.getData().toString());
        setResult(RESULT_OK, resultIntent);
    }


    private void handleSaveFailure (Throwable e) {
        String msg = getString(R.string.save_tree_failure);
        Logger.error(msg, e);
        safeDismiss(saveDialog);
        Toast.makeText(App.getAppInstance(), msg, Toast.LENGTH_SHORT).show();
    }

    private void safeDismiss(ProgressDialog dialog) {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Is the intent in add tree mode?
     */
    private boolean addMode() {
        return (getIntent().getStringExtra("new_tree") != null) && getIntent().getStringExtra("new_tree").equals("1");
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // In order to allow Fields to handle activity results themselves, share the activity
            // result with all of the FieldGroups, which will dispatch to the appropriate fields
            // based on the keys in the intent data
            case FIELD_ACTIVITY_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    for (FieldGroup group : App.getFieldManager().getFieldGroups()) {
                        group.receiveActivityResult(resultCode, data, this);
                    }
                }
                break;
            case TREE_MOVE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        plot.setData(new JSONObject(data.getStringExtra("plot")));
                    } catch (JSONException e) {
                        Logger.error(e);
                    }
                    plotLocation = getPlotLocation(plot);
                    showPositionOnMap();
                }
                break;

            case PHOTO_USING_CAMERA_RESPONSE:
                if (resultCode == RESULT_OK) {
                    changePhotoUsingCamera(outputFilePath);
                }
                break;
            case PHOTO_USING_GALLERY_RESPONSE:
                if (resultCode == RESULT_OK) {
                    changePhotoUsingGallery(data);
                }
                break;
        }

    }


    // This function is called at the end of the whole camera process. You might
    // want to call your rc.submit method here, or store the bm in a class level
    // variable.
    protected void submitBitmap(Bitmap bm) {
        this.newTreePhoto = bm;
        changeFAB();
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "Image Successfully Added", Snackbar.LENGTH_LONG);

        View snackbarLayout = snackbar.getView();
        TextView textView = (TextView)snackbarLayout.findViewById(android.support.design.R.id.snackbar_text);
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        textView.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.padding_small));
        snackbar.show();

    }


    protected void changePhotoUsingCamera(String filePath) {
        Bitmap pic = PhotoActivity.getCorrectedCameraBitmap(filePath);
        if (pic != null) {
            submitBitmap(pic);
        }
    }

    protected void changePhotoUsingGallery(Intent data) {
        submitBitmap(PhotoActivity.getCorrectedGalleryBitmap(data));
    }



}
