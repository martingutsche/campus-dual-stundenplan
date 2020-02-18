package de.martin_gutsche.campusdualstundenplan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (mGoogleSignInAccount == null) {
            //Redirect to splashActivity
            startActivity(Intent(this, SplashActivity::class.java))
        } else {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val myWork = PeriodicWorkRequest.Builder(CalendarWorker::class.java, (3 * 60).toLong(), TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(this)
                    .enqueueUniquePeriodicWork("CampusDualCalendarSync", ExistingPeriodicWorkPolicy.REPLACE, myWork)

            WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("CampusDualCalendarSync")
                    .observe(this, object : Observer<List<WorkInfo>> {

                        private var lastState: WorkInfo.State? = null

                        override fun onChanged(workInfos: List<WorkInfo>) {
                            if (workInfos.isNotEmpty()) {
                                if (workInfos[0].state == WorkInfo.State.ENQUEUED && lastState == WorkInfo.State.RUNNING) {
                                    Toast.makeText(baseContext, getString(R.string.workmsg_finished), Toast.LENGTH_LONG).show()
                                } else if (workInfos[0].state == WorkInfo.State.RUNNING) {
                                    Toast.makeText(baseContext, getString(R.string.workmsg_running), Toast.LENGTH_LONG).show()
                                }
                                lastState = workInfos[0].state
                            }
                        }
                    })
        }
    }
}