package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PatientAppDataSingleton {

    private static PatientAppDataSingleton instance;
    private Map<String, JSONArray> userData;
    private PatientAppDataSingleton() {
        userData = new HashMap<>();
    }
    // Method to get the singleton instance
    public static synchronized PatientAppDataSingleton getInstance() {
        if (instance == null) {
            instance = new PatientAppDataSingleton();
        }
        return instance;
    }

    public JSONArray getUserData(String username) {
        return userData.getOrDefault(username, new JSONArray());
    }

    public void addUserData(String username, JSONObject newUserObject) {
        JSONArray userMessages = userData.getOrDefault(username, new JSONArray());
        // Determine the type of message
        String messageKey = null;
        if (newUserObject.has("message")) {
            messageKey = "message";
        } else if (newUserObject.has("image_base64")) {
            messageKey = "image_base64";
        } else if (newUserObject.has("audio_data")) {
            messageKey = "audio_data";
        }

        if (messageKey == null) {
            return;
        }
        userMessages.put(newUserObject);
        userData.put(username, userMessages);
    }
}