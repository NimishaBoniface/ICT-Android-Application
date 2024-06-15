package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Use SocketClientTask to send registration request to the server
        new SocketClientTask(this, "register").execute(username, password);
    }

    public void handleServerResponse(String response) {
        if (response.contains("success")) {
            Intent intent = new Intent(this, MainActivity.class); // Navigate back to MainActivity
            intent.putExtra("username", usernameEditText.getText().toString());
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(this, "Registration  failed. Try again...", Toast.LENGTH_SHORT).show();
        }
    }
}