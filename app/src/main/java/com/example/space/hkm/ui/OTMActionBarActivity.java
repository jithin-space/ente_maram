package com.example.space.hkm.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.example.space.hkm.App;

/***
 * Custom class for app-wide changes to the Action Bar.
 *
 * This class will grow over time as new overrides are added.
 */
public class OTMActionBarActivity extends AppCompatActivity {
    @Override
    public void onResume() {
        super.onResume();
        // Change the title depending on whether or not
        // an instance is active.
        this.setTitle(App.getInstanceName());
    }

    @Override
    public void onBackPressed(){
        App app = App.getAppInstance();
        Intent intent = new Intent(app,InstanceSwitcherActivity.class );
        startActivity(intent);
        finish();
    }

}
