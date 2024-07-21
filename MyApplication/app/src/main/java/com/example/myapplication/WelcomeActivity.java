package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class WelcomeActivity extends Fragment {
    private Button patientLoginButton, doctorLoginButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_welcome, container, false);


        patientLoginButton = view.findViewById(R.id.patientLoginButton);
        doctorLoginButton = view.findViewById(R.id.doctorLoginButton);

        patientLoginButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("role", "patient");
            PatientLoginActivity loginFragment = new PatientLoginActivity();
            loginFragment.setArguments(bundle);
            ((MainActivity) getActivity()).navigateToFragment(loginFragment);
        });
        doctorLoginButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("role", "doctor");
            DoctorLoginActivity doctorloginFragment = new DoctorLoginActivity();
            doctorloginFragment.setArguments(bundle);
            ((MainActivity) getActivity()).navigateToFragment(doctorloginFragment);
        });
        return view;
    }


}