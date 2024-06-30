package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button loginButton, registerButton;

    private ImageButton backButton;

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);

        role = getIntent().getStringExtra("role");

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Use SocketClientTask to send login request to the server
        new SocketClientTask(this, "login").execute(username, password, role);

    }

    public void handleServerResponse(String response) {
        // Handle server response for login
        if (response.contains("success")) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            Intent intent;
            if ("doctor".equals(role)) {
                intent = new Intent(this, DoctorActivity.class);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
//                    String csvId = jsonResponse.getString("audio_csv_id");
//                    intent.putExtra("audio_csv_id", csvId);

                    JSONArray data = jsonResponse.getJSONArray("data");
                    AppDataSingleton.getInstance().setData(data);

//                    intent.putExtra("data", data.toString());

//                    HashSet<String> uniqueUsernames = new HashSet<>();
//                    for (int i = 0; i < data.length(); i++) {
//                        JSONObject jsonObject = data.getJSONObject(i);
//                        uniqueUsernames.add(jsonObject.getString("username"));
//                    }
//
//                    JSONArray uniqueUsernamesArray = new JSONArray();
//                    for (String username : uniqueUsernames) {
//                        uniqueUsernamesArray.put(username);
//                    }
//
//                    intent.putExtra("unique_usernames", uniqueUsernamesArray.toString());
//                    intent.putExtra("data", data.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                intent = new Intent(this, MessagingActivity.class);
                intent.putExtra("username", usernameEditText.getText().toString());
            }
            startActivity(intent);
        } else {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}