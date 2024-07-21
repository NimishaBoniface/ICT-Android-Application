package com.example.myapplication;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class DoctorActivity  extends Fragment implements SocketResponseHandler,ConnectionListener {
    private LinearLayout dataArea;
    private Map<String, Button> userButtons = new HashMap<>();
    private LinearLayout linearLayout;
    private SocketManager socketManager;
    private String doctorUsername;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_doctor, container, false);
        if (getArguments() != null) {
            doctorUsername = getArguments().getString("username");
        }
        ImageButton backButton = view.findViewById(R.id.backButton);
        linearLayout = view.findViewById(R.id.linearLayout);
        backButton.setOnClickListener(this::onBackButtonClicked);
        populatePatientList();
        establishConnection();
        return view;
    }

    private void establishConnection() {
        try {
            TrustManager[] trustManagers = createTrustManagers();
            socketManager = SocketManager.getInstance();
            socketManager.createSocket(doctorUsername,trustManagers, this, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populatePatientList() {
        DoctorAppDataSingleton appData = DoctorAppDataSingleton.getInstance();
        Set<String> uniquenames = appData.getUsernames();
        for (String patientUsername : uniquenames) {
            Button button = new Button(requireContext());
            button.setText("View data for " + patientUsername);
            userButtons.put(patientUsername, button);
            button.setOnClickListener(v -> {
                UserDetailActivity userDetailActivity = new UserDetailActivity();
                Bundle bundle = new Bundle();
                bundle.putString("patientUsername", patientUsername);
                bundle.putString("doctorUsername", doctorUsername);
                userDetailActivity.setArguments(bundle);
                ((MainActivity) getActivity()).navigateToFragment(userDetailActivity);
            });
            linearLayout.addView(button);
        }
    }

    private TrustManager[] createTrustManagers() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca;
        try (InputStream caInput = requireContext().getAssets().open("certificate.crt")) {
            ca = cf.generateCertificate(caInput);
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        return tmf.getTrustManagers();
    }
    private void addDataToView(String data) {
        TextView textView = new TextView(requireContext());
        textView.setText(data);
        textView.setPadding(16, 8, 16, 8);
        dataArea.addView(textView);
    }
    @Override
    public void onConnected() {}

    @Override
    public void onDisconnected() {
        socketManager.sendMessage(doctorUsername, "disconnect", null, null, null, null, null);
    }

    @Override
    public void onConnectionFailed(Exception e) {}
    @Override
    public void handleServerResponse(String response) {
     getActivity().runOnUiThread(() -> {
            try {
                if(response != null){
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("username") && jsonResponse.has("message") ) {
                        String message = jsonResponse.getString("message");
                        String username = jsonResponse.getString("username");
                        DoctorAppDataSingleton.getInstance().addUserData(username, jsonResponse);
                        addUserButton(username);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void addUserButton(String username) {
        if (!userButtons.containsKey(username)) {  // Only add button if it doesn't exist
            Button button = new Button(requireContext());
            button.setText("View data for " + username);
            button.setOnClickListener(v -> {
                UserDetailActivity userDetailActivity = new UserDetailActivity();
                Bundle bundle = new Bundle();
                bundle.putString("patientUsername", username);
                bundle.putString("doctorUsername", doctorUsername);
                userDetailActivity.setArguments(bundle);
                ((MainActivity) getActivity()).navigateToFragment(userDetailActivity);

            });
            linearLayout.addView(button);
            userButtons.put(username, button);  // Add button to the map
        }
    }

    public void onBackButtonClicked(View view) {
        if (socketManager != null) {
            socketManager.disconnect(doctorUsername, this, this);
        }
        DoctorLoginActivity loginActivity = new DoctorLoginActivity();
        Bundle bundle = new Bundle();
        bundle.putString("role", "doctor");
        loginActivity.setArguments(bundle);
        ((MainActivity) getActivity()).navigateToFragment(loginActivity);
    }
}