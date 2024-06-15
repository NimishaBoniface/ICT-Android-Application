package com.example.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClientTask extends AsyncTask<String, Void, String> {
    private static final String SERVER_IP = "10.0.2.2"; // Replace with your server IP
    private static final int SERVER_PORT = 1234;
    private MessagingActivity messagingActivity;
    private RegisterActivity registerActivity;
    private Context context;
    private String action;

    public SocketClientTask(Context context) {
        this.context = context;
    }

    public SocketClientTask(Context context, String action) {
        this.context = context;
        this.action = action;
    }

    @Override
    protected String doInBackground(String... params) {
        JSONObject jsonObject = new JSONObject();
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (context instanceof MessagingActivity) {
                String message = params[0];
                jsonObject.put("message", message);

            } else if(context instanceof LoginActivity||context instanceof RegisterActivity) {
                String username = params[0];
                String password = params[1];


                try {
                    jsonObject.put("action", action);
                    jsonObject.put("username", username);
                    jsonObject.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
            String data = jsonObject.toString();
            writer.println(data);
            String response = reader.readLine(); // Read server response
            socket.close();
            return response;
        } catch (Exception e) {
            Log.e("SocketClientTask", "Error", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String response) {
        if (context instanceof RegisterActivity) {
            ((RegisterActivity) context).handleServerResponse(response);
        } else if (context instanceof LoginActivity) {
            ((LoginActivity) context).handleServerResponse(response);
        } else if (context instanceof MessagingActivity) {
            ((MessagingActivity) context).handleServerResponse(response);
        } else {
            messagingActivity.handleServerResponse("Failed to connect to server");
        }
    }

    public void sendMessageToServer(String message) {
        execute(message);
    }
}
