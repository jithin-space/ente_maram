package com.example.space.hkm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.LoginManager;
import com.example.space.hkm.R;
import com.example.space.hkm.data.User;
import com.example.space.hkm.helpers.Logger;
import com.example.space.hkm.rest.RequestGenerator;
import com.example.space.hkm.rest.handlers.LoggingJsonHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by space on 7/2/18.
 */

public class Register extends Activity {
    private final LoginManager loginManager = App.getLoginManager();
    private ProgressDialog dialog;

    private  String USERNAME="";
    private  String PASSWORD = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
    }

    /*
     * UI Event Handlers
     */

    public void redirectLogin(View view){
        startActivity(new Intent(this, LoginActivity.class));
    }
    public void handleContinueClick(View view) {
        String password = ((EditText) findViewById(R.id.register_password)).getText().toString();
        String email = ((EditText) findViewById(R.id.register_username)).getText().toString();

        String phoneNumber = ((EditText) findViewById(R.id.register_email)).getText().toString();
        String password2 = ((EditText) findViewById(R.id.register_password2)).getText().toString();
        String firstName = ((EditText) findViewById(R.id.register_firstName)).getText().toString();
        String lastName = ((EditText) findViewById(R.id.register_lastName)).getText().toString();
//        String phone = ((EditText) findViewById(R.id.phone)).getText().toString();
        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
            alert(R.string.all_fields_required);
        } else if (!validEmail(email)) {
            alert(R.string.invalid_email);
        } else if (!strongPassword(password)) {
            alert(R.string.new_passwords_not_strong);
        } else if (!password2.equals(password)) {
            alert(R.string.new_passwords_dont_match);
        } else {
             USERNAME=email;
             PASSWORD=password;
//            startActivity(TermsOfService.getIntent(this,username, email, password, firstName, lastName));
            handleRegisterClick(email,password,phoneNumber,firstName,lastName);
        }
    }

    public void handleRegisterClick(String email, String password, String phoneNumber, String firstName, String lastName) {
        RequestGenerator rc = new RequestGenerator();
        User model = null;
        dialog = ProgressDialog.show(Register.this, "", "Creating User Account...", true, true);

        try {
            model = new User(email, firstName, lastName, email, password,phoneNumber);
        } catch (JSONException e) {
            Logger.error("error in User JSON.", e);
            showErrorAndGoBack();
            dialog.dismiss();
        }

        try {
            rc.register(App.getAppInstance(), model, registrationResponseHandler);
        } catch (Exception e) {
            Logger.error(e);
            showErrorAndGoBack();
            dialog.dismiss();
        }
    }


    /*
    * Response handlers
    */
    private final JsonHttpResponseHandler registrationResponseHandler = new LoggingJsonHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

            String username = USERNAME;
            String password = PASSWORD;

            if (responseIsSuccess(response)) {
                loginManager.logIn(App.getAppInstance(), username, password, msg -> {
                    Bundle data = msg.getData();
                    if (data.getBoolean("success")) {
                        dialog.dismiss();
                        notifyUserThatAcctCreatedAndReturnToProfile();
                        return true;
                    } else {
                        showErrorAndGoBack();
                        return false;
                    }
                });
            } else {
                Logger.warning("Problem creating user account");
                showErrorAndGoBack();
            }
        }

        @Override
        public void failure(Throwable e, String response) {
            dialog.dismiss();
            if (responseIsConflict(e, response)) {
                Toast.makeText(Register.this, R.string.username_is_taken, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Logger.warning("Problem creating user account", e);
                showErrorAndGoBack();
            }
        }
    };

    private static boolean responseIsConflict(Throwable t, String response) {
        return response.equals("CONFLICT");
    }

    private static boolean responseIsSuccess(JSONObject response) {
        String status = "";
        try {
            status = response.getString("status");
        } catch (JSONException e) {
            Logger.error(e);
        }
        return status.equals("success");
    }


    private void showErrorAndGoBack() {
        Toast.makeText(Register.this, R.string.problem_creating_account, Toast.LENGTH_SHORT).show();
        finish();
    }

    /*
     * Helper functions to display info to the user
     */
    private void notifyUserThatAcctCreatedAndReturnToProfile() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.done_registering)
                .setMessage(R.string.done_registering_msg)
                .setPositiveButton(
                        R.string.OK,
                        (dialog1, which) -> startActivity(new Intent(App.getAppInstance(),
                                App.hasInstanceCode() ?
                                        InstanceSwitcherActivity.class :
                                        InstanceSwitcherActivity.class
                        ))
                )
                .show();
    }


    private static boolean validEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
//for future use
    private static boolean isValidPhoneNumber(String phoneNumber){
        return Patterns.PHONE.matcher(phoneNumber).matches();
    }

    private static boolean strongPassword(String password) {
        return password.length() >= 6;
    }

    private void alert(@StringRes int msg) {
        alert(this.getString(msg));
    }

    private void alert(String msg) {
        Toast.makeText(App.getAppInstance(), msg, Toast.LENGTH_LONG).show();
    }


}
