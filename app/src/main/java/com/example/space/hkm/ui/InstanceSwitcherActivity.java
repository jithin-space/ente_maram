package com.example.space.hkm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.adapters.InstanceInfoArrayAdapter;
import com.example.space.hkm.data.InstanceInfo;
import com.example.space.hkm.data.User;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.LoggingJsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class InstanceSwitcherActivity extends Activity {


    private static final int MINIMUM_DISTANCE_IN_METERS = 100;
    private static final int INSTANCE_SELECT_REQUEST_CODE = 1;

    private User user = null;
    private Location userLocation;

    LocationManager locationManager;

    private ProgressDialog loadingInstances;
    private ProgressDialog loadingInstance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instance_switcher_activity);

//        findViewById(R.id.login_button).setOnClickListener(v -> startActivity(new Intent(InstanceSwitcherActivity.this, LoginActivity.class)));
//        findViewById(R.id.logout_button).setOnClickListener(v -> {
//            App.getLoginManager().logOut(InstanceSwitcherActivity.this);
//            startActivity(new Intent(InstanceSwitcherActivity.this, LoginActivity.class));
//        });
//        findViewById(R.id.public_instances_button).setOnClickListener(v ->
//                startActivityForResult(
//                        new Intent(InstanceSwitcherActivity.this, PublicInstanceListDisplay.class),
//                        INSTANCE_SELECT_REQUEST_CODE));
//
        locationManager = (LocationManager) App.getAppInstance().getSystemService(Context.LOCATION_SERVICE);
    }

    // TODO: backport this algorithm, it's more fault tolerant
    private Location getBestLocation(Criteria accuracyCrit) {
        Location location = null;

        if (locationManager != null) {
            final String bestProvider = locationManager.getBestProvider(accuracyCrit, true);
            List<String> providers = locationManager.getProviders(accuracyCrit, true);

            if (bestProvider != null) {
                Collections.sort(providers, (s1, s2) -> s1.equals(bestProvider) ? 1 :
                        s2.equals(bestProvider) ? -1 : 0);
            }

            for (String provider : providers) {
                location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    break;
                }
            }
        }

        return location;
    }

    /**
     * Returns true if the two locations are far away enough to requery for nearby instances,
     * or if one is an invalid location
     */
    private static boolean areLocationsDistant(Location a, Location b) {
        return (a == null || b == null) || a.distanceTo(b) > MINIMUM_DISTANCE_IN_METERS;
    }


    @Override
    public void onStart() {
        super.onStart();

        // Only setup instance lists if the logged in user has changed or moved from their location
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        final Location newLocation = getBestLocation(criteria);
        final User newUser = App.getLoginManager().loggedInUser;

        if (user != newUser || areLocationsDistant(userLocation, newLocation)) {
            reloadNearbyInstanceList(newLocation);
        }

        userLocation = newLocation;
        user = newUser;

//        updateAccountElements();
    }

//    private void updateAccountElements() {
//        TextView userNameView = (TextView) findViewById(R.id.username_text);
//        LoginManager loginManager = App.getLoginManager();
//
//        if (loginManager.isLoggedIn()) {
//            findViewById(R.id.have_account_text).setVisibility(View.GONE);
//            findViewById(R.id.login_button).setVisibility(View.GONE);
//            try {
//                userNameView.setText(makeUserNameString(loginManager.loggedInUser.getUserName()));
//                userNameView.setVisibility(View.VISIBLE);
//            } catch (JSONException e) {
//                Logger.error("Could not get username.", e);
//            }
//            findViewById(R.id.logout_button).setVisibility(View.VISIBLE);
//        } else {
//            findViewById(R.id.have_account_text).setVisibility(View.VISIBLE);
//            findViewById(R.id.login_button).setVisibility(View.VISIBLE);
//            userNameView.setVisibility(View.GONE);
//            findViewById(R.id.logout_button).setVisibility(View.GONE);
//        }
//    }

    public ArrayList<InstanceInfo> inflateForKey(JSONObject data, String key) {
        JSONArray instances = data.optJSONArray(key);
        return InstanceInfo.getInstanceInfosFromJSON(instances);
    }

    private void resetInstanceListViews(Location newLocation) {
        findViewById(R.id.instance_list_retry).setVisibility(View.VISIBLE);
        findViewById(R.id.alterBox).setVisibility(View.VISIBLE);
        if (loadingInstances != null) {
            loadingInstances.dismiss();
        }

        if (newLocation == null) {

            findViewById(R.id.instance_list_location_off).setVisibility(View.VISIBLE);
            findViewById(R.id.instance_list_none_found).setVisibility(View.GONE);
        } else {

            findViewById(R.id.instance_list_location_off).setVisibility(View.GONE);
            findViewById(R.id.instance_list_none_found).setVisibility(View.VISIBLE);
        }


    }

    public void reload(View view){
        finish();
        startActivity(getIntent());
    }


    private void reloadNearbyInstanceList(Location newLocation) {
        RequestGenerator rg = new RequestGenerator();

        // TODO: Rather than using 0,0 when the GPS is off, we should hit a different endpoint to retrieve just "my tree maps"
        double latitude = 0;
        double longitude = 0;

        if(newLocation != null)
        {
            latitude = newLocation.getLatitude();
            longitude = newLocation.getLongitude();
        }


        loadingInstances = ProgressDialog.show(this, getString(R.string.instance_switcher_dialog_heading),
                getString(R.string.instance_switcher_loading_instances));

        final ListView instancesView = (ListView) findViewById(R.id.instance_list);
        instancesView.setEmptyView(findViewById(R.id.instance_list_empty));

        if(newLocation != null){
            rg.getInstancesNearLocation(latitude, longitude,
                    new LoggingJsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                            final LinkedHashMap<CharSequence, List<InstanceInfo>> instances = new LinkedHashMap<>();
                            instances.put(getString(R.string.my_tree_maps), inflateForKey(data, "personal"));
                            instances.put(getString(R.string.neary_tree_maps), inflateForKey(data, "nearby"));

                            InstanceInfoArrayAdapter adapter =
                                    new InstanceInfoArrayAdapter(instances, InstanceSwitcherActivity.this, newLocation);

                            instancesView.setAdapter(adapter);

                            resetInstanceListViews(newLocation);

                            instancesView.setOnItemClickListener((parent, v, position, id) -> {
                                InstanceInfo instance = adapter.getItem(position).value;
                                redirectToTabLayout(instance);
                            });
                        }




                        @Override
                        public void failure(Throwable e, String message) {
                            resetInstanceListViews(newLocation);
                            new AlertDialog.Builder(InstanceSwitcherActivity.this)
                                    .setTitle(R.string.request_failed)
                                    .setMessage(R.string.instance_switcher_request_failed)
                                    .setNeutralButton(R.string.reload_instance_list,
                                            (dialog, which) -> reloadNearbyInstanceList(newLocation))
                                    .setCancelable(true)
                                    .show();
                        }
                    }
            );
        }else{
            resetInstanceListViews(null);
//            new AlertDialog.Builder(InstanceSwitcherActivity.this)
//                    .setTitle(R.string.request_failed)
//                    .setMessage(R.string.instance_switcher_request_failed)
//                    .setNeutralButton(R.string.reload_instance_list,
//                            (dialog, which) -> reloadNearbyInstanceList(newLocation))
//                    .setCancelable(true)
//                    .show();
        }


    }

    private void redirectToTabLayout(InstanceInfo instance) {
        loadingInstance = ProgressDialog.show(this,
                getString(R.string.instance_switcher_dialog_heading),
                getString(R.string.instance_switcher_loading_instance));
        String instanceCode = instance.getUrlName();
        App.reloadInstanceInfo(instanceCode, new RedirectCallback());
    }

    private class RedirectCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            Intent intent = new Intent(InstanceSwitcherActivity.this, TabLayoutOTM.class);
            InstanceSwitcherActivity.this.startActivity(intent);

            if (loadingInstance != null) {
                loadingInstance.dismiss();
            }
            InstanceSwitcherActivity.this.finish();
            return true;
        }
    }


}

