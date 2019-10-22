package de.martin_gutsche.campusdualstundenplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest myWork =
                    new PeriodicWorkRequest.Builder(CalendarWorker.class, 3 * 60, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                            .build();

            WorkManager.getInstance(this)
                    .enqueueUniquePeriodicWork("CampusDualCalendarSync", ExistingPeriodicWorkPolicy.REPLACE, myWork);

            WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("CampusDualCalendarSync")
                    .observe(this, new Observer<List<WorkInfo>>() {

                        private WorkInfo.State lastState;

                        @Override
                        public void onChanged(List<WorkInfo> workInfos) {
                            if (!workInfos.isEmpty() && workInfos.get(0) != null) {
                                if (workInfos.get(0).getState() == WorkInfo.State.ENQUEUED && lastState == WorkInfo.State.RUNNING) {
                                    Toast.makeText(getBaseContext(), getString(R.string.workmsg_finished), Toast.LENGTH_LONG).show();
                                } else if (workInfos.get(0).getState() == WorkInfo.State.RUNNING) {
                                    Toast.makeText(getBaseContext(), getString(R.string.workmsg_running), Toast.LENGTH_LONG).show();
                                }
                                lastState = workInfos.get(0).getState();
                            }
                        }
                    });
        }
    }
}