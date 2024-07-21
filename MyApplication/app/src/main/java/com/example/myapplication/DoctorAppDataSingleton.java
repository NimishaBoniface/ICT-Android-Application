package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DoctorAppDataSingleton {
    private static DoctorAppDataSingleton instance;
    private Map<String, JSONArray> userData;
    private DoctorAppDataSingleton() {
        userData = new HashMap<>();
    }

    public static synchronized DoctorAppDataSingleton getInstance() {
        if (instance == null) {
            instance = new DoctorAppDataSingleton();
        }
        return instance;
    }
    public JSONArray getUserData(String username) {
        return userData.getOrDefault(username, new JSONArray());
    }

    // Method to add new data for a user
    public void addUserData(String username, JSONObject newUserObject) {
        JSONArray userMessages = userData.getOrDefault(username, new JSONArray());
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
    public Set<String> getUsernames() {
        return userData.keySet();
    }
}