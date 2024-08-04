package com.example.myapplication;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
public class SocketManager {
    private static final String SERVER_IP = "10.0.2.2";
//    private static final String SERVER_IP = "192.168.0.5";

//    private static final String SERVER_IP = "172.23.244.113";

    private static final int SERVER_PORT = 1234;
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Map<String, UserSocket> userSockets = new HashMap<>();
    private long sendTimestamp;

    private SocketManager() {

    }
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }
    private class UserSocket {
        SSLSocket socket;
        PrintWriter writer;
        BufferedReader reader;
        boolean isConnected = false;
        SocketResponseHandler responseHandler;
        ConnectionListener connectionListener;
        UserSocket(SocketResponseHandler handler, ConnectionListener connectionListener) {
            this.responseHandler = handler;
            this.connectionListener = connectionListener;
        }

        void connect(String username, TrustManager[] trustManagers) {
            new Thread(() -> {
                if (!isConnected) {
                    try {
                        SSLContext sslContext = createSSLContext(trustManagers);
                        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                        socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);
                        OutputStream outputStream = socket.getOutputStream();
                        writer = new PrintWriter(outputStream, true);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        isConnected = true;
                        listenForMessages(username, trustManagers);
                        Log.d(TAG, "Socket connected for user: " + username);
                        if (connectionListener != null) {
                            connectionListener.onConnected();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error connecting to socket for user: " + username, e);
                        if (connectionListener != null) {
                            connectionListener.onConnectionFailed(e);
                        }
                    }
                } else {
                    // Already connected, invoke the callback immediately
                    if (connectionListener != null) {
                        connectionListener.onConnected();
                    }
                }
            }).start();
        }

        void listenForMessages(String username, TrustManager[] trustManagers) {
//            System.out.println("Recieved Timestamp.................................: " + System.currentTimeMillis());
            new Thread(() -> {
                try {
                    String response;
                    while (isConnected && !socket.isClosed() && (response = reader.readLine()) != null) {
                        responseHandler.handleServerResponse(response);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "reconnect ", e);
                    reconnect(username, trustManagers);
                }
            }).start();
        }

        void sendMessage(String action, String username, String password, String role, String message, String audioCsvData, String encodedImage, long sendTimestamp) {
            long sendTime = System.currentTimeMillis();
//            long sendTime = Calendar.getInstance().getTimeInMillis();
            System.out.println("Send Timestamp............................: " + sendTime);
            new Thread(() -> {
                if (isConnected) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        switch (action) {
                            case "sendMessage":
                                jsonObject.put("username", username);
                                jsonObject.put("message", message);
                                jsonObject.put("timestamp", sendTime);
                                break;
                            case "login":
                                jsonObject.put("action", action);
                                jsonObject.put("username", username);
                                jsonObject.put("password", password);
                                if (role != null) {
                                    jsonObject.put("role", role);
                                }
                                break;
                            case "register":
                                jsonObject.put("action", action);
                                jsonObject.put("username", username);
                                jsonObject.put("password", password);
                                if (role != null) {
                                    jsonObject.put("role", role);
                                }
                                break;
                            case "sendAudio":
                                jsonObject.put("username", username);
                                jsonObject.put("audio_csv", audioCsvData);
                                jsonObject.put("timestamp", sendTime);
                                break;
                            case "sendImage":
                                jsonObject.put("username", username);
                                jsonObject.put("image_base64",  encodedImage);
                                jsonObject.put("timestamp", sendTime);
                                break;
                        }
                        String data = jsonObject.toString();
                        Log.d(TAG, "Sending message: " + data);
                        System.out.println();
                        writer.println(data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Socket is not connected for user: " + username);
                }
            }).start();
        }

        void disconnect(String username ) {
            new Thread(() -> {
                if (isConnected) {
                    try {
                        // Construct a disconnection message
                        JSONObject disconnectMessage = new JSONObject();
                        try {
                            disconnectMessage.put("action", "disconnect");
                            disconnectMessage.put("username", username);  // Assuming 'username' is a field or can be passed
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON error preparing disconnect message", e);
                        }

                        // Send the disconnect message to the server
                        if (writer != null) {
                            writer.println(disconnectMessage.toString());
                            writer.flush();
                        }

                        // Give the server a moment to process the disconnection
                        try {
                            Thread.sleep(100);  // 100 milliseconds for the server to process
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }

                        // Safely close the writer
                        if (writer != null) {
                            writer.close();
                            writer = null;
                        }

                        // Safely close the reader
                        if (reader != null) {
                            reader.close();
                            reader = null;
                        }

                        // Safely close the socket
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }

                        isConnected = false;
                        Log.d(TAG, "Successfully disconnected socket for " + username);
                    } catch (IOException e) {
                        Log.e(TAG, "Error disconnecting socket", e);
                    }
                }
            }).start();
        }

        void updateHandlers(SocketResponseHandler handler, ConnectionListener connectionListener) {
            this.responseHandler = handler;
            this.connectionListener = connectionListener;
        }

        void reconnect(String username, TrustManager[] trustManagers) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to close existing socket during reconnect", e);
            }
            isConnected = false;
            Log.d(TAG, "Reconnecting...");
            connect(username, trustManagers);
        }
    }
    private SSLContext createSSLContext(TrustManager[] trustManagers) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        return sslContext;
    }
    public void createSocket(String username, TrustManager[] trustManagers, SocketResponseHandler handler, ConnectionListener connectionListener) {
        UserSocket userSocket = userSockets.get(username);
        if (userSocket == null) {
            userSocket = new UserSocket(handler, connectionListener);
            userSockets.put(username, userSocket);
            userSocket.connect(username, trustManagers);
        } else {
            userSocket.updateHandlers(handler, connectionListener);
            if (userSocket.isConnected) {
                connectionListener.onConnected();
            } else {
                userSocket.connect(username, trustManagers);
            }
        }
    }
    public void sendMessage(String username, String action, String password, String role, String message, String audioCsvData,String encodedImage) {
        UserSocket userSocket = userSockets.get(username);
        sendTimestamp = System.currentTimeMillis();
        if (userSocket != null) {
            if (action.equals("disconnect")) {
                userSocket.disconnect(username);  // Ensure disconnection after sending the message
                userSockets.remove(username);
            }else{
                userSocket.sendMessage(action, username, password, role, message, audioCsvData,  encodedImage,sendTimestamp);
            }

        } else {
            Log.e(TAG, "No socket found for user: " + username);
        }
    }
    public void disconnect(String username, SocketResponseHandler handler, ConnectionListener connectionListener) {
        UserSocket userSocket = userSockets.get(username);
        if (userSocket != null && userSocket.isConnected) {
            connectionListener.onDisconnected();

        }
    }


}
