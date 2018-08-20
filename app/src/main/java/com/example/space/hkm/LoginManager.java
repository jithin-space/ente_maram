package com.example.space.hkm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.example.space.hkm.data.User;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.RestHandler;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.ui.LoginActivity;

import org.json.JSONException;
import cz.msebera.android.httpclient.client.HttpResponseException;


/**
 * Created by space on 6/28/18.
 */

public class LoginManager {
    private static final String MESSAGE_KEY = "message";
    private static final String USER_KEY = "user";
    private static final String PASS_KEY = "pass";

    public User loggedInUser = null;

    private final Context context;
    private final SharedPreferences prefs;

    public LoginManager(Context context) {
        this.context = context;
        this.prefs = getPreferences();
    }

    public void autoLogin(final Handler.Callback callback) {
        String user = prefs.getString(USER_KEY, null);
        String pass = prefs.getString(PASS_KEY, null);
        if (user != null && pass != null) {
            logIn(context, user, pass, msg -> {
                Bundle data = msg.getData();
                if (data == null || !data.getBoolean(RestHandler.SUCCESS_KEY)) {
                    logOut();
                }
                return callback.handleMessage(msg);
            });
        } else {
            Message msg = Message.obtain();
            Bundle args = new Bundle();
            args.putBoolean(RestHandler.SUCCESS_KEY, false);
            msg.setData(args);

            callback.handleMessage(msg);
        }
    }

    /**
     * Store new password in user preferences
     */
    public void storePassword(String newpass) {
        prefs.edit().putString(PASS_KEY, newpass).commit();
    }


    public void logIn(final Context activityContext, final String username, final String password,
                      final Handler.Callback callback) {

        final RequestGenerator rg = new RequestGenerator();

        rg.logIn(activityContext, username, password, new RestHandler<User>(new User()) {

            final Message resultMessage = new Message();

            private void handleCallback(Bundle data) {
                resultMessage.setData(data);

                if (App.hasInstanceCode()) {
                    App.reloadInstanceInfo(msg -> {
                        callback.handleMessage(resultMessage);
                        return true;
                    });
                } else {
                    callback.handleMessage(resultMessage);
                }
            }

            @Override
            public void failure(Throwable e, String message) {
                final Bundle data = new Bundle();
                data.putBoolean(SUCCESS_KEY, false);
                data.putString(MESSAGE_KEY, activityContext.getString(R.string.could_not_connect));
                if (e instanceof HttpResponseException) {
                    final HttpResponseException actualError = (HttpResponseException) e;
                    if (actualError.getStatusCode() == 401) {
                        data.putString(MESSAGE_KEY, activityContext.getString(R.string.incorrect_username_or_password));
                    }
                }

                handleCallback(data);
                rg.cancelRequests(activityContext);
            }

            /**
             * Store new password in user preferences
             */
            public void storePassword(String newpass) {
                prefs.edit().putString(PASS_KEY, newpass).commit();
            }

            @Override
            public void dataReceived(User response) {
                final Bundle data = new Bundle();
                prefs.edit().putString(USER_KEY, username).commit();
                storePassword(password);
//                App.getAppInstance().setUserOnAnalyticsTracker(response);

                loggedInUser = response;
                try {
                    loggedInUser.setPassword(password);
                } catch (JSONException e) {
                    Logger.error("Error setting user password", e);
                }

                data.putBoolean(SUCCESS_KEY, true);
                handleCallback(data);
            }
        });
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }


    public void logOut(Activity activity) {
        logOut();
        if (!App.hasSkinCode()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
    }

    private void logOut() {
        // Previous instance info is now invalid
        App.removeCurrentInstance();

        loggedInUser = null;
        prefs.edit().remove(USER_KEY).commit();
        prefs.edit().remove(PASS_KEY).commit();

    }


    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
    }

}




