package de.martin_gutsche.campusdualstundenplan

import android.annotation.SuppressLint
import android.content.Context
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import de.martin_gutsche.campusdualstundenplan.Util.httpGet
import de.martin_gutsche.campusdualstundenplan.Util.httpPost
import org.jsoup.Jsoup
import java.net.CookieHandler
import java.net.CookieManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class CampusDualUser(private var username: String, private var hash: String) {
    val lectures: List<CampusDualCalendarEvent>
        get() {
            // times actually don't matter to campus dual
            val currentTime = System.currentTimeMillis()
            val start = currentTime - 60 * 60 * 24 * 14
            val end = currentTime + 60 * 60 * 24 * 31 * 12
            val responseString = httpGet("$SS_URL/room/json", hashMapOf(
                    "userid" to username,
                    "hash" to hash,
                    "start" to "" + start,
                    "end" to "" + end,
                    "_" to "" + currentTime
            ))

            return try {
                Klaxon().parseArray(responseString)!!
            } catch (e: KlaxonException) {
                Klaxon().parseArray("[]")!!
            }
        }

    init {
        allowAllCerts()
    }

    companion object {
        private const val ERP_URL = "https://erp.campus-dual.de"
        private const val SS_URL = "https://selfservice.campus-dual.de"

        fun createWithPassword(username: String, password: String, context: Context): CampusDualUser {
            allowAllCerts()

            // without a CookieManager campus dual doesn't recognise us when we want to get the hash
            val manager = CookieManager()
            CookieHandler.setDefault(manager)

            //initial Request to get the hidden fields (especially "sap-login-XSRF")
            val initUrl = ERP_URL + "/sap/bc/webdynpro/sap/zba_initss?" +
                    "sap-client=100" +
                    "&sap-language=de" +
                    "&uri=https://selfservice.campus-dual.de/index/login"

            val initPage = Jsoup.parse(httpGet(initUrl))
            val hiddenInputs = initPage.select("#SL__FORM > input[type=hidden]")

            //login request
            val params = hashMapOf("sap-user" to username, "sap-password" to password)
            for (input in hiddenInputs) {
                params[input.attr("name")] = input.attr("value")
            }

            val loginUrl = ERP_URL + initPage.select("#SL__FORM").attr("action")
            httpPost(loginUrl, params)

            //Request of the main Page to get the hash needed to get a json calendar
            val mainResponse = httpGet("$SS_URL/index/login")

            val index = mainResponse.indexOf(" hash=\"") // needs whitespace to match just one result
            if (index != -1) {
                val hash = mainResponse.substring(index + 7, index + 7 + 32)
                Util.saveLoginData(username, hash, context)
                return CampusDualUser(username, hash)
            } else {
                throw RuntimeException("No hash was included in the Response -> login data is probably wrong")
            }
        }


        /**
         * This Method is sadly needed, as the server doesn't send the complete CA chain
         */
        private fun allowAllCerts() {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
                }
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        }
    }
}