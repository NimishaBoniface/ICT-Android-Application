package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
public class UserDetailActivity extends Fragment implements SocketResponseHandler,ConnectionListener{
    private short[] audioData;
    private Button btnPlayAudio;
    private ImageButton backButton;
    private LinearLayout messageArea;
    private SocketManager socketManager;
    private String doctorUsername;
    String patientUsername;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_userdetail, container, false);
        if (getArguments() != null) {
            patientUsername  = getArguments().getString("patientUsername");
            doctorUsername  = getArguments().getString("doctorUsername");
        }
        backButton = view.findViewById(R.id.backButton);

        messageArea = view.findViewById(R.id.messageArea);
        socketManager = SocketManager.getInstance();
        DoctorAppDataSingleton appData = DoctorAppDataSingleton.getInstance();
        JSONArray userMessages = appData.getUserData(patientUsername);
        displayMessages(userMessages, patientUsername);
        backButton.setOnClickListener(v -> onBackButtonClicked(v));

        boolean hasAudioData = false;
        try {
            TrustManager[] trustManagers = createTrustManagers();
            socketManager.createSocket(doctorUsername, trustManagers, this, this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to create TrustManager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return view;
    }
    @Override
    public void onDisconnected() {}
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
    @Override
    public void onConnected() {}

    @Override
    public void onConnectionFailed(Exception e) {}

    @Override
    public void handleServerResponse(String response) {
        getActivity().runOnUiThread(() -> {
            try {
                if(response != null){
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("username")) {
                        String username = jsonResponse.getString("username");
                        DoctorAppDataSingleton.getInstance().addUserData(username, jsonResponse);
                        updateMessages(jsonResponse, username);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        long receiveTimestamp = System.currentTimeMillis();
    }
    private void updateMessages(JSONObject messageData, String username) {
        try {
            if (messageData.getString("username").equals(username)) {
                if (messageData.has("message")) {
                    long sendTimestamp = messageData.getLong("timestamp");
                    long receiveTimestamp = System.currentTimeMillis();
                    long latency = (receiveTimestamp - sendTimestamp) ;// Calculate latency
                    // Debug prints to verify the timestamps
                    System.out.println("Latency in receiving text message for  the connected user:"  + latency + " ms");
                    addMessage(messageData.getString("message"));
                }
                if (messageData.has("audio_data")) {
                     long sendTimestamp = messageData.getLong("timestamp");
                    long receiveTimestamp = System.currentTimeMillis();
                    long latency = (receiveTimestamp - sendTimestamp) ;// Calculate latency
                    // Debug prints to verify the timestamps
                    System.out.println("Latency in receiving audio message for  the connected user:"  + latency + " ms");
                    String audioDataString = messageData.getString("audio_data");
                    convertAudioData(audioDataString);
                    addPlayButton();
                }
                else if (messageData.has("image_base64")) {
                    long sendTimestamp = messageData.getLong("timestamp");
                    long receiveTimestamp = System.currentTimeMillis();
                    long latency = (receiveTimestamp - sendTimestamp) ;// Calculate latency
                    // Debug prints to verify the timestamps
                    System.out.println("Latency in receiving image message for  the connected user:"  + latency + " ms");
                    String encodedImage = messageData.getString("image_base64");
                    addImageToView(encodedImage);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void displayMessages(JSONArray messages, String username) {
        boolean hasAudioData = false;
        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject jsonObject = messages.getJSONObject(i);

                if (jsonObject.getString("username").equals(username)) {
                    if (jsonObject.has("message")) {
                        String message = jsonObject.getString("message");
                        addMessage(message + "\n");
                    }
                    if (jsonObject.has("audio_data")) {
                        String audioDataString = jsonObject.getString("audio_data");
                        convertAudioData(audioDataString);
                        hasAudioData = true;
                        addPlayButton();
                    }
                    if (jsonObject.has("image_base64")) {
                        String encodedImage = jsonObject.getString("image_base64");
                        addImageToView(encodedImage);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private void addPlayButton() {
        LinearLayout buttonLayout = new LinearLayout(requireContext());
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.START);
        int heightInPixels = (int) (48 * getResources().getDisplayMetrics().density + 0.5f);
        Button playButton = new Button(requireContext());
        playButton.setText("Play Audio");
        playButton.setBackgroundResource(R.drawable.custom_button_background); // Set the custom background
        playButton.setPadding(16, 8, 16, 8); // Add padding
        playButton.setCompoundDrawablePadding(8);
        LinearLayout.LayoutParams playButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels
        );
        playButtonParams.setMargins(0, 16, 16, 16);
        playButtonParams.gravity = Gravity.START; // Align button to the left
        playButton.setLayoutParams(playButtonParams);
        playButton.setOnClickListener(v -> playAudio());

        Button downloadButton = new Button(requireContext());
        downloadButton.setText("Download Audio");
        downloadButton.setBackgroundResource(R.drawable.custom_button_background); // Set the custom background
        downloadButton.setPadding(16, 8, 16, 8); // Add padding
        downloadButton.setCompoundDrawablePadding(8);
        LinearLayout.LayoutParams downloadButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels
        );
        downloadButtonParams.setMargins(0, 16, 16, 16);
        downloadButtonParams.gravity = Gravity.START; // Align button to the left
        downloadButton.setLayoutParams(downloadButtonParams);// Add your download icon here
        downloadButton.setOnClickListener(v -> downloadAudio());

        buttonLayout.addView(playButton);
        buttonLayout.addView(downloadButton);
        messageArea.addView(buttonLayout);
    }
    private void downloadAudio() {
        String path = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/recording_" + System.currentTimeMillis()+"_"+patientUsername + ".wav";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            writeWaveFileHeader(fos, 44100, 1, 16);
            byte[] byteData = shortToByte(audioData, audioData.length);
            fos.write(byteData, 0, byteData.length);
            updateWaveFileHeader(path, 44100, 1, 16);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateWaveFileHeader(String filePath, long sampleRate, int channels, int bitDepth) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long fileSize = raf.length();
            long totalDataLen = fileSize - 8;
            long totalAudioLen = fileSize - 44;
            long byteRate = sampleRate * channels * bitDepth / 8;
            raf.seek(4);
            raf.write((int) (totalDataLen & 0xff));
            raf.write((int) ((totalDataLen >> 8) & 0xff));
            raf.write((int) ((totalDataLen >> 16) & 0xff));
            raf.write((int) ((totalDataLen >> 24) & 0xff));
            raf.seek(40);
            raf.write((int) (totalAudioLen & 0xff));
            raf.write((int) ((totalAudioLen >> 8) & 0xff));
            raf.write((int) ((totalAudioLen >> 16) & 0xff));
            raf.write((int) ((totalAudioLen >> 24) & 0xff));
        }
    }
    private void addImageToView(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        int heightInPixels = (int) (48 * getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout horizontalLayout = new LinearLayout(requireContext());
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels));
        horizontalLayout.setGravity(Gravity.START);


        // Create and configure the download ImageButton
        Button downloadButton = new Button(requireContext());
        downloadButton.setText("Download image..."); // Use your drawable resource
        LinearLayout.LayoutParams downloadButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels);
        downloadButton.setBackgroundResource(R.drawable.custom_button_background); // Set the custom background
        downloadButton.setPadding(16, 8, 16, 8); // Add padding
        downloadButton.setCompoundDrawablePadding(8);
        downloadButtonParams.setMargins(0, 16, 0, 16);
        downloadButton.setLayoutParams(downloadButtonParams);
        horizontalLayout.addView(downloadButton);

        downloadButton.setOnClickListener(v -> {
            saveImageToDownloads(decodedByte);
        });
        messageArea.addView(horizontalLayout);
    }
    private void saveImageToDownloads(@NonNull Bitmap bitmap) {
        File downloadDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null) {
            File imageFile = new File(downloadDir, "image_" + System.currentTimeMillis() +"_"+ patientUsername+ ".png");
            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(requireContext(), "Image saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Download directory not found", Toast.LENGTH_SHORT).show();
        }
    }
    private void addMessage(String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(16);
        textView.setPadding(16, 8, 16, 8);
        textView.setTextColor(getResources().getColor(R.color.textColorPrimary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16);
        textView.setBackgroundResource(R.drawable.user_message_background);
        params.gravity = Gravity.START;
        textView.setLayoutParams(params);
        messageArea.addView(textView);
    }
    public void onBackButtonClicked(View view) {

        DoctorActivity doctorActivity = new DoctorActivity();
        Bundle bundle = new Bundle();
        bundle.putString("username", doctorUsername);
        doctorActivity.setArguments(bundle);
        ((MainActivity) getActivity()).navigateToFragment(doctorActivity);
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

    private void writeWaveFileHeader(FileOutputStream out, long sampleRate, int channels, int bitDepth) throws IOException {
        byte[] header = new byte[44];
        long totalDataLen = 36;
        long byteRate = sampleRate * channels * bitDepth / 8;
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * bitDepth / 8); // block align
        header[33] = 0;
        header[34] = (byte) bitDepth; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = 0;
        header[41] = 0;
        header[42] = 0;
        header[43] = 0;
        out.write(header, 0, 44);
    }
    private byte[] shortToByte(short[] data, int length) {
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            bytes[i * 2] = (byte) (data[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (data[i] >> 8);
        }
        return bytes;
    }
}