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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class RegisterFragment extends Fragment implements SocketResponseHandler, ConnectionListener{

    private TextInputLayout usernameTextInputLayout, passwordTextInputLayout;
    private TextInputEditText usernameEditText, passwordEditText;
    private Button registerButton;
    private ImageButton backButton;
    private SocketManager socketManager;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_register, container, false);
        socketManager = SocketManager.getInstance();
        usernameTextInputLayout = view.findViewById(R.id.usernameTextInputLayout);
        passwordTextInputLayout = view.findViewById(R.id.passwordTextInputLayout);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        registerButton = view.findViewById(R.id.registerButton);
        backButton = view.findViewById(R.id.backButton);
        registerButton.setEnabled(false);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
        backButton.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
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
                // Only show the error if there is some text and it's less than the required length
                if (editText.isFocused() && s.length() > 0 && s.length() < minLength) {
                    layout.setError("Must be at least " + minLength + " characters");
                } else {
                    layout.setError(null);
                }
                updateRegisterButtonState();
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

    private void registerUser() {
        String username = usernameEditText.getText().toString();

        try {
            TrustManager[] trustManagers = createTrustManagers();
        socketManager.createSocket(username, trustManagers,this, this) ;
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
        socketManager.sendMessage(username, "register", password, null, null, null, null);
    }

    @Override
    public void onDisconnected() {}

    @Override
    public void onConnectionFailed(Exception e) {}

   @Override
    public void handleServerResponse(String response) {
       getActivity().runOnUiThread(() -> {
           if (getActivity() == null) return;
           if (response.contains("success")) {
               PatientLoginFragment loginFragment = new PatientLoginFragment();
               Bundle bundle = new Bundle();
               bundle.putString("username", usernameEditText.getText().toString());
               bundle.putString("role", "patient");
               loginFragment.setArguments(bundle);
               ((MainActivity) getActivity()).navigateToFragment(loginFragment);
           } else {

               try {
                   JSONObject jsonResponse = new JSONObject(response);
                   String message = jsonResponse.getString("message");
                   usernameTextInputLayout.setError(message);
               } catch (JSONException e) {
                   e.printStackTrace();
                   usernameTextInputLayout.setError("Error parsing response");
               }
           }
       });
    }

    private void updateRegisterButtonState() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        boolean usernameIsValid = username.length() >= 3;
        boolean passwordIsValid = password.length() >= 8;

        // Enable the register button only if both username and password are valid
        registerButton.setEnabled(usernameIsValid && passwordIsValid);
    }
}