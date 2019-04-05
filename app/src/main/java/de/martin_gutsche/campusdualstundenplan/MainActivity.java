package de.martin_gutsche.campusdualstundenplan;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInAccount mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mGoogleSignInAccount == null) {
            //Redirect to splashActivity
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            JSONObject loginData = Util.getLoginData(this);

            CampusDualUser campusDualUser = null;
            try {
                String username = loginData.getString("username");
                String hash = loginData.getString("hash");
                campusDualUser = new CampusDualUser(username, hash);
            } catch (JSONException e) {
                //Redirect to splashActivity
                startActivity(new Intent(this, SplashActivity.class));
            }


            //TODO schedule work
        }
    }
}
