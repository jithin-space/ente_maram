package com.example.space.hkm.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.rest.handlers.RestHandler;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.ArrayList;
import java.util.List;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_SCREEN_DELAY_MILLIS = 2000;
    public static final int MULTIPLE_PERMISSIONS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Launcher);
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_splash_screen);
//        WebView wv = (WebView) findViewById(R.id.splash_webview);
//        wv.loadUrl("file:///android_asset/splash_content.html");
          checkPermissions();


    }





    private  void checkPermissions() {
        int result;
        String[] permissionArrays = new String[]{Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,

        };
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissionArrays) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );

        }else{
            gotoNext();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        Boolean isdenied=false;
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;
                            isdenied=true;

                        }
                        if(this.shouldShowRequestPermissionRationale(per)){
//                            alertView();
                        }

                    }
                    // Show permissionsDenied
                    if(isdenied){
                        alertView();
                    }else{

                        gotoNext();


                    }

                }
                return;
            }
        }
    }

    public void gotoNext(){
        final Deferred<Bundle, Throwable, Integer> autoLoginDeferred =
                new DeferredObject<>();

        final Promise<Bundle, Throwable, Integer> autoLogin = autoLoginDeferred.promise();

        App.getLoginManager().autoLogin(msg -> {
            autoLoginDeferred.resolve(msg.getData());
            return true;
        });

        Handler splashHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                autoLogin.done(args -> {
                    // Only skip the instance switcher for a skinned app
                    // or if the login succeeded on a non-skinned app
                    if (args != null && args.getBoolean(RestHandler.SUCCESS_KEY)) {

                        redirect(true);
                    } else {

                        redirect(false);
                    }
                });



//                redirect(App.hasSkinCode());
            }
        };
        splashHandler.sendMessageDelayed(Message.obtain(), SPLASH_SCREEN_DELAY_MILLIS);



    }




    private void alertView() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("Permission Denied")

                .setMessage("Without those permission the app is unable to function properly.Are you sure you want to deny this permission?")

                .setNegativeButton("I'M SURE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        dialoginterface.dismiss();
                    }
                })
                .setPositiveButton("RE-TRY", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        dialoginterface.dismiss();
                       checkPermissions();
                    }
                }).show();
    }

    private void redirect(final Boolean skipInstanceSwitcher) {
        App app = App.getAppInstance();

        Intent intent = new Intent(app,skipInstanceSwitcher
                        ? InstanceSwitcherActivity.class
                        : LoginActivity.class
        );

        startActivity(intent);
        finish();



    }


}




