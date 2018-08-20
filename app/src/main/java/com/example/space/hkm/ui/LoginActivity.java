package com.example.space.hkm.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.example.space.hkm.App;
import com.example.space.hkm.LoginManager;
import com.example.space.hkm.rest.handlers.RestHandler;
import com.example.space.hkm.R;

public class LoginActivity extends Activity {

    private final LoginManager loginManager = App.getLoginManager();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EditText password = (EditText) findViewById(R.id.login_password);

        password.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login(textView);
                return true;
            }
            return false;
        });
    }

    public void login(View view) {
        String username = ((EditText) findViewById(R.id.login_username)).getText().toString().trim();
        String password = ((EditText) findViewById(R.id.login_password)).getText().toString().trim();

        if (validate(username, password)) {
            requestLogin(username, password);
        }
    }

    private boolean validate(String user, String pass) {
        if ("".equals(user) || "".equals(pass)) {
            String msg = "Please enter both a Username and a Password";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void requestLogin(String username, String password) {
        final ProgressDialog dialog = ProgressDialog.show(this, "", "Logging in...", true, true);
        loginManager.logIn(this, username, password, msg -> {
            dialog.cancel();
            Bundle data = msg.getData();
            if (data.getBoolean(RestHandler.SUCCESS_KEY)) {
                setResult(RESULT_OK);

                App app = App.getAppInstance();
                Intent intent = new Intent(app,InstanceSwitcherActivity.class );
                startActivity(intent);
                finish();
                return true;
            } else {
                Toast.makeText(App.getAppInstance(), data.getString("message"), Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    public void register(View view) {
        startActivity(new Intent(this, Register.class));
    }


}
