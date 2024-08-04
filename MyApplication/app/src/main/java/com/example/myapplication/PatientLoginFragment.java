package com.example.myapplication;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class PatientLoginFragment extends Fragment implements SocketResponseHandler, ConnectionListener {

    private TextInputLayout usernameTextInputLayout, passwordTextInputLayout;
    private TextInputEditText usernameEditText, passwordEditText;
    private Button loginButton, registerButton;
    private ImageButton backButton;
    private String role;
    private SocketManager socketManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_login, container, false);

        if (getArguments() != null) {
            role = getArguments().getString("role");
        }

        // Initialize UI elements
        usernameTextInputLayout = view.findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = view.findViewById(R.id.passwordTextInputLayout);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        registerButton = view.findViewById(R.id.registerButton);
        backButton = view.findViewById(R.id.backButton);
        socketManager = SocketManager.getInstance();
        // Set up the login button state and listeners
        loginButton.setEnabled(false);
        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> {
            RegisterFragment registerFragment = new RegisterFragment();
            ((MainActivity) getActivity()).navigateToFragment(registerFragment);
        });
        backButton.setOnClickListener(v -> {
            WelcomeFragment welcomeActivity = new WelcomeFragment();
            ((MainActivity) getActivity()).navigateToFragment(welcomeActivity);
        });

        setupEditText(usernameEditText, usernameTextInputLayout, 3);
        setupEditText(passwordEditText, passwordTextInputLayout, 8);

        return view;
    }
    private void setupEditText(TextInputEditText editText, TextInputLayout layout, int minLength) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
                if (editText.isFocused() && s.length() > 0 && s.length() < minLength) {
                    layout.setError("Must be at least " + minLength + " characters");
                } else {
                    layout.setError(null);
                }
                updateLoginButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (editText.getText().length() > 0 && editText.getText().length() < minLength) {
                    layout.setError("Must be at least " + minLength + " characters");
                } else {
                    layout.setError(null);
                }
            }
        });
    }
    private void updateLoginButtonState() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        boolean usernameIsValid = username.length() >= 3;
        boolean passwordIsValid = password.length() >= 8;

        loginButton.setEnabled(usernameIsValid && passwordIsValid);
    }
    private void loginUser() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Use SocketClientTask to send login request to the server
        try {
        TrustManager[] trustManagers = createTrustManagers();
        socketManager.createSocket(username, trustManagers, this, this);
        } catch (Exception e) {
            e.printStackTrace();
            }
    }

    private TrustManager[] createTrustManagers() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca;
        try (InputStream caInput = getActivity().getAssets().open("certificate.crt")) {
            ca = cf.generateCertificate(caInput);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        return tmf.getTrustManagers();
    }
    @Override
    public void onConnected() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        socketManager.sendMessage(username, "login", password, role, null, null,null);
    }
    @Override
    public void onConnectionFailed(Exception e) {}
    @Override
    public void onDisconnected() {}
    @Override
    public void handleServerResponse(String response) {
        getActivity().runOnUiThread(() -> {
                if (response.contains("success")) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONArray data = jsonResponse.getJSONArray("data");
                            for (int i = 0; i < data.length(); i++) {
                                try {
                                    JSONObject userObject = data.getJSONObject(i);
                                    String username = userObject.getString("username");
                                    PatientAppDataSingleton.getInstance().addUserData(username, userObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        MessagingFragment messagingActivity = new MessagingFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("username", usernameEditText.getText().toString());
                        bundle.putString("role", "patient");
                        messagingActivity.setArguments(bundle);
                        ((MainActivity) getActivity()).navigateToFragment(messagingActivity);

                } else {
                     try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String message = jsonResponse.getString("message");
                        usernameTextInputLayout.setError(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        usernameTextInputLayout.setError("User not Registered");
                    }
                }
    });
   }
}