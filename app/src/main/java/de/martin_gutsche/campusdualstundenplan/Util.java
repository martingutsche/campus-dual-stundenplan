package de.martin_gutsche.campusdualstundenplan;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Util {
    public static JSONObject getUserdata(Context context) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        context.openFileInput(context.getString(R.string.userdata_path))))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
}
