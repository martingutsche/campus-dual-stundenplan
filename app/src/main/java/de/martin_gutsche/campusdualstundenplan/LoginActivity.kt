package de.martin_gutsche.campusdualstundenplan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class LoginActivity : AppCompatActivity() {
    private lateinit var inputMatrikelNr: EditText
    private lateinit var inputPassword: EditText

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // make link clickable
        val signUp: TextView = findViewById(R.id.link_sign_up)
        signUp.movementMethod = LinkMovementMethod.getInstance()

        // input fields
        inputMatrikelNr = findViewById(R.id.input_matrikel_rr)
        inputMatrikelNr.requestFocus()
        inputPassword = findViewById(R.id.input_password)
        inputPassword.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        inputMatrikelNr.error = null
        inputPassword.error = null

        val password = inputPassword.text.toString()
        val matrikelnummer = if (inputMatrikelNr.text.isNotBlank() && inputMatrikelNr.text.toString().substring(0, 1) == "s") {
            inputMatrikelNr.text.toString().substring(1)
        } else {
            inputMatrikelNr.text.toString()
        }

        var cancel = false
        var focusView: View? = null

        // Check for validity
        if (password == "") {
            inputPassword.error = getString(R.string.error_empty_password)
            focusView = inputPassword
            cancel = true
        }

        if (matrikelnummer == "") {
            inputMatrikelNr.error = getString(R.string.error_empty_matrikelnummer)
            focusView = inputMatrikelNr
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            val loginTask = LoginTask(matrikelnummer, password, this)
            loginTask.execute()
        }
    }

    override fun onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true)
    }

    internal class LoginTask(private val matrikelnummer: String, private val password: String, activity: Activity) : AsyncTask<Void, Void, Boolean>() {
        private val activityRef: WeakReference<Activity> = WeakReference(activity)

        override fun onPreExecute() {
            val activity = activityRef.get()!!

            // Disable UI elements
            activity.findViewById<View>(R.id.btn_login).isEnabled = false
            activity.findViewById<View>(R.id.input_matrikel_rr).isEnabled = false
            activity.findViewById<View>(R.id.input_password).isEnabled = false

            // Hide Keyboard
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val windowToken = activity.window.decorView.rootView.windowToken
            imm.hideSoftInputFromWindow(windowToken, 0)
        }

        override fun doInBackground(vararg params: Void): Boolean? {
            try {
                CampusDualUser.createWithPassword(matrikelnummer, password, activityRef.get() as Context)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }

        override fun onPostExecute(success: Boolean?) {
            val activity = activityRef.get()
            if (activity != null) {
                if (success!!) {
                    activity.startActivity(Intent(activity, MainActivity::class.java))
                    activity.finish()
                } else {
                    activity.findViewById<View>(R.id.btn_login).isEnabled = true
                    activity.findViewById<View>(R.id.input_matrikel_rr).isEnabled = true
                    activity.findViewById<View>(R.id.input_password).isEnabled = true
                    Toast.makeText(
                            activity.baseContext,
                            activity.getString(R.string.login_failed),
                            Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onCancelled() {
            val activity = activityRef.get()
            if (activity != null) {
                activity.findViewById<View>(R.id.btn_login).isEnabled = true
                activity.findViewById<View>(R.id.input_matrikel_rr).isEnabled = true
                activity.findViewById<View>(R.id.input_password).isEnabled = true
            }
        }
    }
}