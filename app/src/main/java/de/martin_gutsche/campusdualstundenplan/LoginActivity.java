package de.martin_gutsche.campusdualstundenplan;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity {
    private EditText inputMatrikelNr;
    private EditText inputPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // input fields
        inputMatrikelNr = findViewById(R.id.input_matrikel_rr);
        inputMatrikelNr.requestFocus();
        inputPassword = findViewById(R.id.input_password);
        inputPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    private void attemptLogin() {
        inputMatrikelNr.setError(null);
        inputPassword.setError(null);

        String matrikelnummer = inputMatrikelNr.getText().toString();
        String password = inputPassword.getText().toString();

        if (matrikelnummer.substring(0, 1).equals("s")) {
            matrikelnummer = matrikelnummer.substring(1);
        }


        boolean cancel = false;
        View focusView = null;

        // Check for validity
        if (password.equals("")) {
            inputPassword.setError(getString(R.string.error_empty_password));
            focusView = inputPassword;
            cancel = true;
        }

        if (matrikelnummer.equals("")) {
            inputMatrikelNr.setError(getString(R.string.error_empty_matrikelnummer));
            focusView = inputMatrikelNr;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            LoginTask loginTask = new LoginTask(matrikelnummer, password, this);
            loginTask.execute();
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    static class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String matrikelnummer;
        private final String password;
        private final WeakReference<Activity> activityRef;

        LoginTask(String matrikelnummer, String password, Activity activity) {
            this.matrikelnummer = matrikelnummer;
            this.password = password;
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = activityRef.get();

            // Disable UI elements
            activity.findViewById(R.id.btn_login).setEnabled(false);
            activity.findViewById(R.id.input_matrikel_rr).setEnabled(false);
            activity.findViewById(R.id.input_password).setEnabled(false);

            // Hide Keyboard
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            IBinder windowToken =
                    activity.getWindow().getDecorView().getRootView().getWindowToken();
            imm.hideSoftInputFromWindow(windowToken, 0);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                new CampusDualUser(matrikelnummer, password, activityRef.get());
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            Activity activity = activityRef.get();
            if (activity != null) {
                if (success) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    activity.finish();
                } else {
                    activity.findViewById(R.id.btn_login).setEnabled(true);
                    activity.findViewById(R.id.input_matrikel_rr).setEnabled(true);
                    activity.findViewById(R.id.input_password).setEnabled(true);
                    Toast.makeText(
                            activity.getBaseContext(),
                            activity.getString(R.string.login_failed),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.findViewById(R.id.btn_login).setEnabled(true);
                activity.findViewById(R.id.input_matrikel_rr).setEnabled(true);
                activity.findViewById(R.id.input_password).setEnabled(true);
            }
        }
    }
}