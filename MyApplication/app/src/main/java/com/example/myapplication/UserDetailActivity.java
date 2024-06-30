package com.example.myapplication;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserDetailActivity extends AppCompatActivity {

    private TextView messageView;
    private short[] audioData;
    private Button btnPlayAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userdetail);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);

        messageView = findViewById(R.id.messageView);
        String username = getIntent().getStringExtra("username");
//        String data = getIntent().getStringExtra("data");


        AppDataSingleton appData = AppDataSingleton.getInstance();
        JSONArray dataArray = appData.getData();
        boolean hasAudioData = false;


        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject jsonObject = dataArray.getJSONObject(i);
                if (jsonObject.getString("username").equals(username)) {
                    if (jsonObject.has("message")) {
                        sb.append(jsonObject.getString("message")).append("\n");
                    }
                    if (jsonObject.has("audio_data")) {
                        String audioDataString = jsonObject.getString("audio_data");
                        convertAudioData(audioDataString);
                        hasAudioData = true;
                    }
                }
            }
            messageView.setText(sb.toString());

            if (hasAudioData) {
                btnPlayAudio.setVisibility(View.VISIBLE);
                btnPlayAudio.setOnClickListener(v -> playAudio());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void convertAudioData(String audioDataString) {
        String[] audioDataArray = audioDataString.split(",");
        audioData = new short[audioDataArray.length];

        for (int i = 0; i < audioDataArray.length; i++) {
            try {
                audioData[i] = Short.parseShort(audioDataArray[i].trim());
            } catch (NumberFormatException e) {

            }
        }
    }

    private void playAudio() {
        int sampleRate = 44100; // Set your sample rate
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO; // Assuming mono audio
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // Assuming 16-bit PCM

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                audioData.length * 2, // Length in bytes, so multiply by 2 for shorts (16-bit)
                AudioTrack.MODE_STATIC
        );

        audioTrack.write(convertShortsToBytes(audioData), 0, audioData.length * 2); // Write shorts to audio track
        audioTrack.play();
    }
    private byte[] convertShortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2]; // 2 bytes per short
        for (int i = 0; i < shorts.length; i++) {
            bytes[i * 2] = (byte) (shorts[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) (shorts[i] >> 8);
        }
        return bytes;
    }
}
