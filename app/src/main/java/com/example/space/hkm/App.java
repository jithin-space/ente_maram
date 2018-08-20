package com.example.space.hkm;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.space.hkm.data.InstanceInfo;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.RestHandler;
import com.google.android.gms.analytics.HitBuilders;

import java.util.ArrayList;

/**
 * Created by space on 6/28/18.
 */

public class App extends Application {
    private static App appInstance = null;
    private LoginManager loginManager = null;
    private static SharedPreferences sharedPreferences = null;

    private static FieldManager fieldManager = null;
    private static FilterManager filterManager = null;

    public static final String LOG_TAG = "HKM_OTM";

    public static final String INSTANCE_CODE = "instance_code";


    private static InstanceInfo currentInstance;
    private static boolean loadingInstance = false;

    private static ArrayList<Handler.Callback> registeredInstanceCallbacks = new ArrayList<>();



    public static App getAppInstance() {
        checkAppInstance();
        return appInstance;
    }

    private static void checkAppInstance() {
        if (appInstance == null) {
            throw new IllegalStateException("Application not created yet");
//            appInstance = new App();
        }
    }

    /**
     * Static access to single field manager instance
     */
    public static FieldManager getFieldManager() {
        return fieldManager;
    }




    public static boolean hasSkinCode() {
        return !TextUtils.isEmpty(appInstance.getString(R.string.skin_code));
    }

    public static boolean hasInstanceCode() {
        return hasSkinCode() || getSharedPreferences().contains(INSTANCE_CODE);
    }


    public static LoginManager getLoginManager() {
        App app = getAppInstance();
        if (app.loginManager == null) {
            app.loginManager = new LoginManager(appInstance);
        }
        return app.loginManager;
    }

    public static SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            checkAppInstance();
            sharedPreferences = appInstance.getSharedPreferences(appInstance.getString(R.string.app_name), Context.MODE_PRIVATE);
            // Set-up SharedPreferences if they haven't been set up before
            setDefaultSharedPreferences(sharedPreferences);
        }
        return sharedPreferences;
    }



    private static void setDefaultSharedPreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        Context context = appInstance.getApplicationContext();
        editor.putString("base_url", context.getString(R.string.base_url))
                .putString("api_url", context.getString(R.string.api_url))
                .putString("tiler_url", context.getString(R.string.tiler_url))
                .putString("plot_feature", context.getString(R.string.plot_feature))
                .putString("boundary_feature", context.getString(R.string.boundary_feature))
                .putString("access_key", context.getString(R.string.access_key))
                .putString("secret_key", context.getString(R.string.secret_key))
                .putString("max_nearby_plots", context.getString(R.string.max_nearby_plots))
                .putString("starting_zoom_level", context.getString(R.string.starting_zoom_level))
                .putString("short_date_format","dd/MM/yyyy")
                .commit();

    }



    @Override
    public void onCreate() {
        super.onCreate();
        appInstance = this;

        getLoginManager();
    }


    /**
     * Clear the current instance info and force a reload of the configured instance
     *
     * @param callback to call when instance is loaded
     */
    public static void reloadInstanceInfo(String instanceCode, Handler.Callback callback) {
        currentInstance = null;
        loadingInstance = true;
        // unnecessary if there is a hard coded instance, can't
        RequestGenerator rg = new RequestGenerator();
        InstanceRefreshHandler handler = new InstanceRefreshHandler(callback);

        rg.getInstanceInfo(instanceCode, handler);
    }





    public static class InstanceRefreshHandler extends RestHandler<InstanceInfo> {
        private Handler.Callback callback;
        private InstanceInfo responseInstanceInfo;

        InstanceRefreshHandler(Handler.Callback callback) {
            this(new InstanceInfo(), callback);
        }

        InstanceRefreshHandler() {
            this(new InstanceInfo(), null);
        }

        InstanceRefreshHandler(InstanceInfo instanceInfo, Handler.Callback callback) {
            super(instanceInfo);
            this.responseInstanceInfo = instanceInfo;
            this.callback = callback;
        }

//        private void handleRegisteredCallbacks(Message msg) {
//            if (registeredInstanceCallbacks.size() > 0) {
//                for (Handler.Callback registeredCallback : registeredInstanceCallbacks) {
//                    registeredCallback.handleMessage(msg);
//                }
//                registeredInstanceCallbacks.clear();
//            }
//        }

        private void handleCallback(boolean success) {
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putBoolean(SUCCESS_KEY, success);
            msg.setData(data);

            // Flag to track if instance request is pending
            loadingInstance = false;

            if (callback != null) {
                callback.handleMessage(msg);
            }

            // In addition to the direct caller, other callbacks may have been
            // registered to be notified of instance loaded status
//            handleRegisteredCallbacks(msg);
        }

        @Override
        public void failure(Throwable e, String message) {
            String errorMessage = appInstance.getString(R.string.cannot_load_instance) +
                    responseInstanceInfo.getName();
            Logger.error(errorMessage, e);
            Toast.makeText(appInstance, errorMessage, Toast.LENGTH_LONG).show();
            handleCallback(false);
        }

        @Override
        public void dataReceived(InstanceInfo instanceInfo) {
            setCurrentInstance(instanceInfo);
            handleCallback(true);
        }
    }

    /**
     * Given the provided instance, create fields and filters
     */
    private static void setCurrentInstance(InstanceInfo currentInstance) {
        App.currentInstance = currentInstance;
        getSharedPreferences().edit().putString(INSTANCE_CODE, currentInstance.getUrlName()).commit();
//        App.getAppInstance().setInstanceOnAnalyticstracker(currentInstance);

        try {
            fieldManager = new FieldManager(currentInstance);
            filterManager = new FilterManager(currentInstance);

        } catch (Exception e) {
            Logger.error("Unable to create field manager from instance", e);
            Toast.makeText(appInstance, "Error setting up OpenTreeMap", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Static access to single search manager instance
     */
    public static FilterManager getFilterManager() {
        return filterManager;
    }


    public static InstanceInfo getCurrentInstance() {
        return currentInstance;
    }

    public static void reloadInstanceInfo(Handler.Callback callback) {
        String hardCodedInstanceCode = appInstance.getString(R.string.skin_code);
        String instanceCode;
        if (!TextUtils.isEmpty(hardCodedInstanceCode)) {
            instanceCode = hardCodedInstanceCode;
        } else if (getSharedPreferences().contains(INSTANCE_CODE)) {
            String neverUsed = "";
            instanceCode = getSharedPreferences().getString(INSTANCE_CODE, neverUsed);
        } else {
            Logger.error("Incorrect state, attempted to reload an instance without an instance code");
            return;
        }
        reloadInstanceInfo(instanceCode, callback);
    }

    /**
     * INSTANCE API
     * <p>
     * TODO: Move to another class.
     */
    public static String getInstanceName() {
        // If this is a skinned app, always return the app name
        // If this is the cloud app, conditionally return either
        // the app name or the instance name, depending on whether
        // the user is connected to an instance.
        String appName = appInstance.getString(R.string.app_name);
        if (hasSkinCode()) {
            return appName;
        } else {
            InstanceInfo currentInstance = App.getCurrentInstance();
            return currentInstance == null ? appName : currentInstance.getName();
        }
    }


    /**
     * Callback to ensure the instance has been loaded, either via a loaded, pending
     * or missing instance info. This method is safe to call at any time to wait for
     * instance info before proceeding with the callback.
     *
     * @param callback
     */
    public void ensureInstanceLoaded(final Handler.Callback callback) {
        if (currentInstance != null) {
            Message msg = Message.obtain();
            Bundle data = new Bundle();
            data.putBoolean("success", true);
            msg.setData(data);

            callback.handleMessage(msg);
        } else {
            // If an instance request is pending, register for a callback on completion,
            // otherwise, force an instance request
            if (loadingInstance) {
                registeredInstanceCallbacks.add(callback);
            } else {
                reloadInstanceInfo(callback);
            }
        }
    }

    public static void removeCurrentInstance() {
        currentInstance = null;
        getSharedPreferences().edit().remove(INSTANCE_CODE).commit();
    }


}

