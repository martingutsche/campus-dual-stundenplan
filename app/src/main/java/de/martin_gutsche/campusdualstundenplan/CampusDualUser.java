package de.martin_gutsche.campusdualstundenplan;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static de.martin_gutsche.campusdualstundenplan.Util.HttpGet;
import static de.martin_gutsche.campusdualstundenplan.Util.HttpPost;

public class CampusDualUser {
    private static final String ERP_URL = "https://erp.campus-dual.de";
    private static final String SS_URL = "https://selfservice.campus-dual.de";
    private String hash;
    private String username;

    /**
     * Constructor if user needs to be logged in.
     */
    CampusDualUser(String username, String password, Context context) throws IOException {
        this.username = username;
        try {
            allowAllCerts();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        login(password, context);
    }

    /**
     * Constructor if user already has his hash and doesn't need to be logged in.
     */
    public CampusDualUser(String username, String hash) {
        this.username = username;
        this.hash = hash;
        try {
            allowAllCerts();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
//        CookieManager manager = new CookieManager(); TODO: remove if everything works
//        CookieHandler.setDefault(manager);
    }

    /**
     * This Method is sadly needed, as the server doesn't send the complete CA chain on connection
     */
    private void allowAllCerts() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });
    }

    /**
     * Log the user in to get the hash for ajax requests on Campus Dual.
     */
    private void login(String password, Context context) throws IOException {
        //initial Request to get the hidden fields (especially "sap-login-XSRF")
        String initUrl = ERP_URL + "/sap/bc/webdynpro/sap/zba_initss?" +
                "sap-client=100" +
                "&sap-language=de" +
                "&uri=https://selfservice.campus-dual.de/index/login";

        String initResponse = HttpGet(initUrl, null, context.getString(R.string.useragent));
        Document initPage = Jsoup.parse(initResponse);
        Elements hiddenInputs = initPage.select("#SL__FORM > input[type=hidden]");

        //login request
        String[][] params = new String[hiddenInputs.size() + 2][2];
                                    //[hiddenInputs.size()+username+password][key+value]
        params[0][0] = "sap-user";
        params[0][1] = URLEncoder.encode(username, "UTF-8");
        params[1][0] = "sap-password";
        params[1][1] = URLEncoder.encode(password, "UTF-8");
        { //don't want to use i in a larger scope
            int i = 2; //0==user; 1==password; 2<=hidden input
            for (Element input : hiddenInputs) {
                //NO ENCODING BECAUSE THE XSRF-TOKEN WOULDN'T STAY THE SAME!!!!
                params[i][0] = input.attr("name");
                params[i][1] = input.attr("value");
                i++;
            }
        }
        String loginUrl = ERP_URL + initPage.select("#SL__FORM").attr("action");
        HttpPost(loginUrl, params, context.getString(R.string.useragent));

        //Request of the main Page to get the hash needed to get a json calendar
        String mainResponse = HttpGet(
                SS_URL + "/index/login",
                null,
                context.getString(R.string.useragent));
        int index = mainResponse.indexOf(" hash=\""); // needs whitespace to match just one result
        hash = mainResponse.substring(index + 7, index + 7 + 32);
        Util.saveLoginData(username, hash, context);
    }

    public JSONArray getNextSemester(Context context)
            throws IOException, JSONException, ParseException {
        long currentTime = System.currentTimeMillis();
        long start = getNextSemesterStart(context);
        long end = (currentTime / 1000) + (60 * 60 * 24 * 31 * 4); //current + 4 months
        String[][] params = {
                {"_", "" + (currentTime - currentTime % (1000 * 60 * 60 * 24))},
                {"start", "" + start},
                {"end", "" + end},
                {"hash", hash},
                {"userid", username}
        };
        String responseString = HttpGet(SS_URL + "/room/json", params, context.getString(R.string.useragent));

        JSONArray responseJSON = null;
        try {
            responseJSON = new JSONArray(responseString);
//            writeToInternal(context.getString(R.string.filename_calendar), responseString, context);
            //TODO save json in some form
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return responseJSON;
    }

    private long getNextSemesterStart(Context context) throws IOException, JSONException, ParseException {
        long semesterStart;
        String[][] params = {
                {"user", username}
        };
        String responseString = HttpGet(SS_URL + "/dash/gettimeline", params, context.getString(R.string.useragent));

        JSONObject response = new JSONObject(responseString);
        JSONArray events = response.getJSONArray("events");

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Calendar cal = new GregorianCalendar(2100, 1, 1, 0, 0, 0); //no user should have earlier semesters
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if (event.getString("title").equals("Theorie")) {
                Date eventStart = sdf.parse(event.getString("start"));
                Date eventEnd = sdf.parse(event.getString("end"));

                if (cal.after(eventStart) && Calendar.getInstance().before(eventEnd)) {
                    cal.setTime(eventStart);
                }
            }
        }
        semesterStart = cal.getTimeInMillis();

        return semesterStart;
    }
}
