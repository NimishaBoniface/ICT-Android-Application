package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DoctorActivity extends AppCompatActivity {

    private LinearLayout dataArea;
    private String csvId;
    private Set<String> uniqueUsernames;
    private JSONArray data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        LinearLayout linearLayout = findViewById(R.id.linearLayout);
        Set<String> uniqueUsernames = new HashSet<>();
//        String response = getIntent().getStringExtra("data");
        AppDataSingleton appData = AppDataSingleton.getInstance();
        JSONArray response = appData.getData();

        try {
            data = new JSONArray(response.toString());
            for (int i = 0; i < data.length(); i++) {
                JSONObject jsonObject = data.getJSONObject(i);
                uniqueUsernames.add(jsonObject.getString("username"));
            }
            for (String username : uniqueUsernames) {
                Button button = new Button(this);
                button.setText("View data for " + username);
                button.setOnClickListener(v -> {
                    Intent intent = new Intent(DoctorActivity.this, UserDetailActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                });
                linearLayout.addView(button);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void addDataToView(String data) {
        TextView textView = new TextView(this);
        textView.setText(data);
        textView.setPadding(16, 8, 16, 8);
        dataArea.addView(textView);
    }
}
