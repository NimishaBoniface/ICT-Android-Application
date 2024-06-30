package com.example.myapplication;

import org.json.JSONArray;

public class AppDataSingleton {

    private static AppDataSingleton instance;
    private JSONArray data; // Example: JSON array to hold your data

    // Private constructor to prevent instantiation from other classes
    private AppDataSingleton() {
        // Initialize your data here if needed
        data = new JSONArray();
    }

    // Method to get the singleton instance
    public static synchronized AppDataSingleton getInstance() {
        if (instance == null) {
            instance = new AppDataSingleton();
        }
        return instance;
    }

    // Getter method for your data
    public JSONArray getData() {
        return data;
    }

    // Setter method for your data
    public void setData(JSONArray newData) {
        this.data = newData;
    }

    // Add other methods as needed to manipulate or access your data
}

