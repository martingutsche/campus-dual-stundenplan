package de.martin_gutsche.campusdualstundenplan;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

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


            ///////////////////////////////////////////////////////////
            System.out.println(" ");
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            openFileInput("test.txt")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("  ");
            ///////////////////////////////////////////////////////////


            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest myWork =
                    new PeriodicWorkRequest.Builder(CalendarWorker.class, 12, TimeUnit.HOURS)
                            .setConstraints(constraints)
                            .build();

            WorkManager.getInstance()
                    .enqueueUniquePeriodicWork("Calendar", ExistingPeriodicWorkPolicy.KEEP, myWork);
        }
    }
}