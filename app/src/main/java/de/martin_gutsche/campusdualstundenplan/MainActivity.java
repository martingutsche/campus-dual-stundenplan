package de.martin_gutsche.campusdualstundenplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
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
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest myWork =
                    new PeriodicWorkRequest.Builder(CalendarWorker.class, 20, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .build();

            WorkManager.getInstance()
                    .enqueueUniquePeriodicWork("Calendar", ExistingPeriodicWorkPolicy.KEEP, myWork);

            WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData("Calendar")
                    .observe(this, new Observer<List<WorkInfo>>() {

                        private WorkInfo.State lastState;

                        @Override
                        public void onChanged(List<WorkInfo> workInfos) {
                            if (workInfos.get(0) != null) {
                                String msg = "Aktualisierungsauftrag is in einem fehlerhaften Status";
                                if (workInfos.get(0).getState() == WorkInfo.State.ENQUEUED) {
                                    if (lastState != WorkInfo.State.RUNNING) {
                                        msg = "Aktualisierungsauftrag ist im System hinterlegt";
                                    } else {
                                        msg = "Aktualisierung abgeschlossen";
                                    }
                                } else if (workInfos.get(0).getState() == WorkInfo.State.RUNNING) {
                                    msg = "Aktualisierung wird gestartet";
                                }
                                lastState = workInfos.get(0).getState();
                                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }
}