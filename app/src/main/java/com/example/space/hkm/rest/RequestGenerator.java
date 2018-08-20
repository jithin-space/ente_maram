package com.example.space.hkm.rest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.example.space.hkm.App;
import com.example.space.hkm.LoginManager;
import com.example.space.hkm.data.InstanceInfo;
import com.example.space.hkm.data.Model;
import com.example.space.hkm.data.Password;
import com.example.space.hkm.data.Plot;
import com.example.space.hkm.data.PlotContainer;
import com.example.space.hkm.data.User;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.rest.handlers.ContainerRestHandler;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by space on 7/2/18.
 */

public class RequestGenerator {
    private RestClient client;
    LoginManager loginManager = App.getLoginManager();

    private static int PHOTOUPLOADTIMEOUT = 30000;

    public RequestGenerator() {
        client = new RestClient();
    }

    public void logIn(Context context, String username, String password,
                      JsonHttpResponseHandler handler) {
        client.getWithAuthentication("/user", username, password, null, handler);
    }

    /**
     * Cancel any pending or active requests started from the provided context
     */
    public void cancelRequests(Context activityContext) {
        client.cancelRequests(activityContext);
    }


    public void register(Context context, User user, JsonHttpResponseHandler handler) {
        client.post(context, "/user", user, handler);
    }

    public void getInstancesNearLocation(double geoY, double geoX,
                                         JsonHttpResponseHandler handler) {
        String url = String.format("locations/%s,%s/instances", geoY, geoX);
        userOptionalGet(url, null, handler);
    }

    /**
     * Request information on a specific OTM instance
     *
     * @param urlName Short URL slug name of instance
     */
    public void getInstanceInfo(String urlName,
                                JsonHttpResponseHandler handler) {

        userOptionalGet("/instance/" + urlName, null, handler);
    }


    /*
     Helper method used to access an endpoint whose return value
     varies by whether the user is logged in.  Try to make the
     request with the user logged in, fall back on non-logged in if
     failure.
    */
    private void userOptionalGet(String url, RequestParams rp, JsonHttpResponseHandler handler) {
        User user = loginManager.loggedInUser;
//
        try {
            rp = new RequestParams();
            rp.put("max", 5);
            rp.put("distance", 3200);
            if (loginManager.isLoggedIn()) {
                client.getWithAuthentication(url,
                        user.getUserName(),
                        user.getPassword(),
                        rp, handler);
            } else {

                client.get(url, rp, handler);
            }
        } catch (JSONException e) {
            Logger.error(e);
            client.get(url, rp, handler);
        }
    }

    public void getAllSpecies(JsonHttpResponseHandler handler) {
        userOptionalGet(getInstanceNameUri("species"), null, handler);
    }

    private String getInstanceNameUri(String path) {
        InstanceInfo instance = App.getAppInstance().getCurrentInstance();
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (instance != null) {
            return "/instance/" + instance.getUrlName() + "/" + path;
        }
        return "";
    }

    public void addPlot(Plot plot, AsyncHttpResponseHandler handler)
            throws JSONException {

        final Plot plotCopy = getPlotCopy(plot);
        client.postWithAuthentication(getInstanceNameUri("plots"),
                loginManager.loggedInUser.getUserName(),
                loginManager.loggedInUser.getPassword(), plotCopy, handler);
    }

    public void addTreePhoto(Plot plot, Bitmap bm,
                             JsonHttpResponseHandler handler)
            throws JSONException {
        String formattedPath = String.format("plots/%s/tree/photo", plot.getId());
        client.postWithAuthentication(getInstanceNameUri(formattedPath), bm,
                loginManager.loggedInUser.getUserName(),
                loginManager.loggedInUser.getPassword(),
                handler, PHOTOUPLOADTIMEOUT);
    }


    private Plot getPlotCopy(Plot plot) throws JSONException {
        // Only send up plot and tree, and only send tree if the tree is there
        final String[] fieldsToCopy = plot.hasTree()
                ? new String[] {"plot", "tree"}
                : new String[] {"plot"};
        final JSONObject copy = new JSONObject(plot.getData(), fieldsToCopy);
        return new Plot(copy);
    }

    public void updatePlot(Plot plot,
                           AsyncHttpResponseHandler handler) {
        if (loginManager.isLoggedIn()) {
            try {
                final Plot plotCopy = getPlotCopy(plot);

                client.putWithAuthentication(getInstanceNameUri("plots/"),
                        loginManager.loggedInUser.getUserName(),
                        loginManager.loggedInUser.getPassword(), plot.getId(), plotCopy, handler);
            } catch (JSONException e) {
                handleBadResponse(e);
            }
        } else {
//            redirectToLoginActivity();
        }
    }
    private void handleBadResponse(JSONException e) {
        Logger.error(e);
    }

    public void getPlotsNearLocation(double geoY, double geoX, RequestParams rp,
                                     ContainerRestHandler<PlotContainer> handler) {

        String url = getInstanceNameUri(String.format("locations/%s,%s/plots", geoY, geoX));
        userOptionalGet(url, rp, handler);

    }

    public void getImage(String imageUrl, BinaryHttpResponseHandler binaryHttpResponseHandler) {
        client.getImage(imageUrl, binaryHttpResponseHandler);
    }


    public void getPlotsNearLocation(double geoY, double geoX, boolean recent, boolean pending,
                                     ContainerRestHandler<PlotContainer> handler) {
        SharedPreferences sharedPrefs = App.getSharedPreferences();
        String maxPlots = sharedPrefs.getString("max_nearby_plots", "10");

        RequestParams params = new RequestParams();
        params.put("max_plots", maxPlots);
        params.put("filter_recent", Boolean.toString(recent));
        params.put("filter_pending", Boolean.toString(pending));

        getPlotsNearLocation(geoY, geoX, params, handler);
    }

    public void changePassword(Context context, String newPassword, JsonHttpResponseHandler handler)
            throws JSONException {
        Model password = new Password(newPassword);

        client.putWithAuthentication("/user/" + loginManager.loggedInUser.getId(),
                loginManager.loggedInUser.getUserName(),
                loginManager.loggedInUser.getPassword(),
                password, handler);
    }









}

