package com.example.myapplication;

public interface ConnectionListener {
    void onConnected();
    void onConnectionFailed(Exception e);
    void onDisconnected();
}
