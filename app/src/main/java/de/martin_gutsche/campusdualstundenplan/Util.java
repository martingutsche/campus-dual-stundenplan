package de.martin_gutsche.campusdualstundenplan;

import android.content.Context;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

class Util {
    /////////////////////////
    // CALENDAR OPERATIONS //
    /////////////////////////
    private static final SimpleDateFormat cdDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMAN);
    /////////////////////
    // FILE OPERATIONS //
    /////////////////////
    private static final String LOGIN_DATA_PATH = "login_data.json";
    private static final String CALENDAR_JSON_PATH = "calendar.json";
    private static final String CALENDAR_ID_PATH = "calendar_id.json";
    ////////////////////////
    // NETWORK OPERATIONS //
    ////////////////////////
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";

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

    static Event convertEventToGoogle(JSONObject cdEvent) throws JSONException, ParseException {
        cdDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

        /// TITLE ///
        String title;
        String cdInst = cdEvent.getString("instructor");
        String cdSinst = cdEvent.getString("sinstructor");
        String cdTitle = cdEvent.getString("title");
        if (cdTitle.contains("-")) {
            title = cdTitle.split("-", 2)[1];
        } else {
            title = cdTitle;
        }
        if (!cdInst.equals("") || !cdSinst.equals("")) {
            if (cdSinst.equals("") || cdInst.equals(cdSinst)) {
                title += " (" + cdInst + ")";
            } else {
                title += " (" + cdInst + ", " + cdSinst + ")";
            }
        }

        ///  ROOM  ///
        String room;
        String cdRoom = cdEvent.getString("room");
        String cdSroom = cdEvent.getString("sroom");
        if (cdSroom.equals("") || cdRoom.equals(cdSroom)) {
            room = cdRoom;
        } else {
            room = cdRoom + " (" + cdSroom + ")";
        }

        ///  DESC  ///
        StringBuilder desc = new StringBuilder();
        String cdDesc = cdEvent.getString("description");
        String cdRemarks = cdEvent.getString("remarks");
        //add description and remarks
        if (cdDesc.equals(cdRemarks)) {
            desc.append(cdDesc);
        } else {
            if (!cdDesc.equals("") && !cdRemarks.equals("")) {
                desc.append(cdDesc);
                desc.append("; ");
                desc.append(cdRemarks);
            } else {
                //ele is only reached when cdDesk or cd cdRemarks is empty (non-exclusive)
                desc.append(cdDesc);
                desc.append(cdRemarks);
            }
        }

        ///  START & END  ///
        String startTime = cdEvent.getString("start");
        DateTime startDateTime = new DateTime(cdDateFormat.parse(startTime));
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Europe/Berlin");

        String endTime = cdEvent.getString("end");
        DateTime endDateTime = new DateTime(cdDateFormat.parse(endTime));
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Europe/Berlin");


        System.out.println(cdDateFormat.parse(endTime));
        System.out.println(endTime);
        System.out.println(end);
        System.out.println();


        ///  CREATE THE ACTUAL EVENT  ///
        return new Event()
                .setSummary(title)
                .setLocation(room)
                .setDescription(desc.toString())
                .setStart(start)
                .setEnd(end)
                .setColorId("8");
    }

    static String HttpGet(String urlString, String[][] params) throws IOException {
        //convert params from HashMap to String formatted to be used in a request
        String paramsString = ParamsToString(params);
        if (!paramsString.equals("")) {
            paramsString = "?" + paramsString;
        }

        //Make the actual connection
        URL url = new URL(urlString + paramsString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);

        //get and return inputStream (converted to a String)
        String responseString = getResponseString(new InputStreamReader(urlConnection.getInputStream()));
        urlConnection.disconnect();
        return responseString;
    }

    static String HttpPost(String urlString, String[][] params) throws IOException {
        //convert params from HashMap to String formatted to be used in a request
        String paramsString = ParamsToString(params);

        //Make the actual connection
        URL url = new URL(urlString);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
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