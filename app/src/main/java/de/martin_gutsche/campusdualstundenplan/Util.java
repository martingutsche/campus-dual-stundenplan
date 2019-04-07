package de.martin_gutsche.campusdualstundenplan;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

class Util {
    // FILE OPERATIONS //
    private static final String LOGIN_DATA_PATH = "login_data.json";
    private static final String CALENDAR_JSON_PATH = "calendar.json";
    private static final String CALENDAR_ID_PATH = "calendar_id.json";

    // login data
    static JSONObject getLoginData(Context context) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        context.openFileInput(LOGIN_DATA_PATH)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (FileNotFoundException | JSONException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void saveLoginData(String username, String hash, Context context) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("hash", hash);
            writeToInternal(LOGIN_DATA_PATH, json.toString(), context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // calendar id
    static String getCalendarString(Context context) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        context.openFileInput(CALENDAR_JSON_PATH)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void saveCalendarString(String calendarString, Context context) {
        writeToInternal(CALENDAR_JSON_PATH, calendarString, context);
    }

    // calendar string
    static String getCalendarId(Context context) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        context.openFileInput(CALENDAR_ID_PATH)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void saveCalendarId(String calendarId, Context context) {
        writeToInternal(CALENDAR_ID_PATH, calendarId, context);
    }

    // helper
    private static void writeToInternal(String path, String data, Context context) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                context.openFileOutput(path, Context.MODE_PRIVATE))) {
            outputStreamWriter.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // NETWORK OPERATIONS //
    static String HttpGet(String urlString, String[][] params, String useragent) throws IOException {
        //convert params from HashMap to String formatted to be used in a request
        String paramsString = ParamsToString(params);
        if (!paramsString.equals("")) {
            paramsString = "?" + paramsString;
        }

        //Make the actual connection
        URL url = new URL(urlString + paramsString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", useragent);

        //get and return inputStream (converted to a String)
        String responseString = getResponseString(new InputStreamReader(urlConnection.getInputStream()));
        urlConnection.disconnect();
        return responseString;
    }

    static String HttpPost(String urlString, String[][] params, String useragent) throws IOException {
        //convert params from HashMap to String formatted to be used in a request
        String paramsString = ParamsToString(params);

        //Make the actual connection
        URL url = new URL(urlString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", useragent);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("Content-Length", String.valueOf(paramsString.length()));

        //write the parameters to the outputStream
        OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
        writer.write(paramsString);
        writer.flush();
        writer.close();

        //get and return inputStream (converted to a String)
        String ResponseString = getResponseString(new InputStreamReader(urlConnection.getInputStream()));
        urlConnection.disconnect();
        return ResponseString;
    }

    private static String ParamsToString(String[][] params) {
        if (params == null) {
            params = new String[0][0];
        }
        StringBuilder paramsStringBuilder = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i != 0) {
                paramsStringBuilder.append("&");
            }

            try {
                paramsStringBuilder
                        .append(URLEncoder.encode(params[i][0], "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(params[i][1], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // this should never happen the encoding is not
                // a variable and UTF-8 should be supported
            }
        }
        return paramsStringBuilder.toString();
    }

    private static String getResponseString(InputStreamReader in) {
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(in)) {
            while ((line = br.readLine()) != null) {
                responseBuilder
                        .append(line)
                        .append("\n");
            }
        } catch (IOException e) {
            //no special directory is accessed, so no problem here
        }
        return responseBuilder.toString();
    }
}