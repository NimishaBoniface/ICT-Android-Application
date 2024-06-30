package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button patientLoginButton, doctorLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        patientLoginButton = findViewById(R.id.patientLoginButton);
        doctorLoginButton = findViewById(R.id.doctorLoginButton);

        patientLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("role", "patient");
            startActivity(intent);
        });

        doctorLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("role", "doctor");
            startActivity(intent);
        });
    }


    private void showPatientLogin() {
        // Show patient login options (you can create a method to handle the patient login)
    }

    private void showPatientRegister() {
        // Show patient registration options (you can create a method to handle the patient registration)
    }

    private void showDoctorLogin() {
        // Show doctor login options (you can create a method to handle the doctor login)
    }

    private void showDoctorRegister() {
        // Show doctor registration options (you can create a method to handle the doctor registration)
    }
}





//    Button  patientButton = findViewById(R.id.patientButton);
//    Button doctorButton = findViewById(R.id.doctorButton);
//    LinearLayout patientOptions = findViewById(R.id.patientOptions);
//    LinearLayout doctorOptions = findViewById(R.id.doctorOptions);
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        findViewById(R.id.patientLoginButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, PatientAuthenticationActivity.class);
//                startActivity(intent);
//            }
//        });
//    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        findViewById(R.id.registerButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
//                startActivity(intent);
//            }
//        });
//
//        findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//                startActivity(intent);
//            }
//        });
//    }


