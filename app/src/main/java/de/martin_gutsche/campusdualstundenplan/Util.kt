package de.martin_gutsche.campusdualstundenplan

import android.content.Context
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

internal object Util {
    /////////////////////
    // FILE OPERATIONS //
    /////////////////////
    private const val LOGIN_DATA_PATH = "login_data.json"
    private const val CALENDAR_JSON_PATH = "calendar.json"
    private const val CALENDAR_ID_PATH = "calendar_id.txt"
    ////////////////////////
    // NETWORK OPERATIONS //
    ////////////////////////
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"

    // login data
    fun getLoginData(context: Context): JSONObject? {
        try {
            context.openFileInput(LOGIN_DATA_PATH).bufferedReader().use {
                return JSONObject(it.readText())
            }
        } catch (e: FileNotFoundException) {
            return null
        }

    }

    fun saveLoginData(username: String, hash: String, context: Context) {
        context.openFileOutput(LOGIN_DATA_PATH, Context.MODE_PRIVATE).use {
            it.write(JSONObject()
                    .put("username", username)
                    .put("hash", hash)
                    .toString().toByteArray())
        }
    }

    // calendar string
    fun getCalendarString(context: Context): String {
        try {
            context.openFileInput(CALENDAR_JSON_PATH).bufferedReader().use {
                return it.readText()
            }
        } catch (e: FileNotFoundException) {
            return "[]"
        }
    }

    fun saveCalendarString(calendarString: String, context: Context) {
        context.openFileOutput(CALENDAR_JSON_PATH, Context.MODE_PRIVATE).use {
            it.write(calendarString.toByteArray())
        }
    }

    // calendar id
    fun getCalendarId(context: Context): String? {
        try {
            context.openFileInput(CALENDAR_ID_PATH).bufferedReader().use {
                return it.readText()
            }
        } catch (e: FileNotFoundException) {
            return null
        }
    }

    fun saveCalendarId(calendarId: String, context: Context) {
        context.openFileOutput(CALENDAR_ID_PATH, Context.MODE_PRIVATE).use {
            it.write(calendarId.toByteArray())
        }
    }

    fun httpGet(urlString: String): String {
        return httpGet(urlString, null)
    }

    fun httpGet(urlString: String, params: HashMap<String, String>?): String {
        //Make the actual connection
        val url = URL(urlString + "?" + paramsToString(params))
        val urlConnection = url.openConnection() as HttpsURLConnection
        urlConnection.setRequestProperty("User-Agent", USER_AGENT)

        //get and return inputStream (converted to a String)
        val responseString = urlConnection.inputStream.bufferedReader().readText()
        urlConnection.disconnect()
        return responseString
    }

    fun httpPost(urlString: String, params: HashMap<String, String>): String {
        //convert params from HashMap to String formatted to be used in a request
        val paramsString = paramsToString(params)

        //Make the actual connection
        val url = URL(urlString)
        val urlConnection = url.openConnection() as HttpsURLConnection
        urlConnection.setRequestProperty("User-Agent", USER_AGENT)
        urlConnection.doOutput = true
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        urlConnection.setRequestProperty("Content-Length", paramsString.length.toString())

        //write the parameters to the outputStream
        val writer = OutputStreamWriter(urlConnection.outputStream)
        writer.write(paramsString)
        writer.flush()
        writer.close()

        //get and return inputStream (converted to a String)
        val responseString = urlConnection.inputStream.bufferedReader().readText()
        urlConnection.disconnect()
        return responseString
    }

    private fun paramsToString(params: HashMap<String, String>?): String {
        if (params == null || params.isEmpty()) return ""

        val sb = StringBuilder()
        params.forEach { (key, value) ->
            sb.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8") + "&")
        }
        val paramStr = sb.toString()
        return paramStr.substring(0, paramStr.length - 1)
    }
}