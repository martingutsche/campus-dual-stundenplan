package de.martin_gutsche.campusdualstundenplan;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    LoginTask loginTask;

    Button btnLogin;
    EditText inputMatrikelNr;
    EditText inputPassword;

    ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // input fields
        inputMatrikelNr = findViewById(R.id.intput_matrikel_rr);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // progressDialog
        progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.login_progress_bar_msg));
    }

    private void attemptLogin() {
        inputMatrikelNr.setError(null);
        inputPassword.setError(null);

        String matrikelnummer = inputMatrikelNr.getText().toString();
        String password = inputPassword.getText().toString();

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
            loginTask = new LoginTask(matrikelnummer, password, this);
            loginTask.execute();
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public class LoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String matrikelnummer;
        private final String password;
        private final Context context;

        LoginTask(String matrikelnummer, String password, Context context) {
            this.matrikelnummer = matrikelnummer;
            this.password = password;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            btnLogin.setEnabled(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                new CampusDualUser(matrikelnummer, password, context);
                return true;
            } catch (Exception e) {
                e.printStackTrace(); //TODO Handle with ExceptionHandler
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            progressDialog.dismiss();
            loginTask = null;
            btnLogin.setEnabled(true);
            if (success) {
                finish();
            } else {
                Toast.makeText(getBaseContext(), getString(R.string.login_failed), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            loginTask = null;
            progressDialog.dismiss();
            btnLogin.setEnabled(true);
        }
    }
}

