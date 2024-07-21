package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
public class MessagingActivity extends Fragment implements SocketResponseHandler,ConnectionListener{
    private static final int REQUEST_CODE_RECORD_AUDIO = 1001;
    private static final int SAMPLE_RATE = 44100;
    private EditText messageEditText;
    private ImageButton attachmentButton, sendButton, voiceMessageButton;
    private LinearLayout messageArea;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private Dialog recordDialog;
    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private int bufferSize;
    private short[] audioData;
    private ImageButton backButton;
    private String username;
    private SocketManager socketManager;
    private String pendingMessage;
    private String pendingAudioCsv;
    private TrustManager[] trustManagers;
    private List<Uri> selectedImages = new ArrayList<>();
    private ActivityResultLauncher<Intent> selectImageLauncher;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_messaging, container, false);

        if (getArguments() != null) {
            username = getArguments().getString("username");
        }
        socketManager = SocketManager.getInstance();
        // Initialize UI components
        messageEditText = view.findViewById(R.id.messageEditText);
        attachmentButton = view.findViewById(R.id.attachmentButton);
        sendButton = view.findViewById(R.id.sendButton);
        voiceMessageButton = view.findViewById(R.id.voiceMessageButton);
        messageArea = view.findViewById(R.id.messageArea);
        backButton = view.findViewById(R.id.backButton);
        PatientAppDataSingleton appData = PatientAppDataSingleton.getInstance();
        JSONArray userMessages = appData.getUserData(username);
        if (userMessages != null && userMessages.length() > 0) {
            displayMessages(userMessages, username);
        }
        // Set up button listeners
        sendButton.setOnClickListener(v -> sendMessage());
        voiceMessageButton.setOnClickListener(v -> showRecordPopup());
        backButton.setOnClickListener(v -> {
            if (socketManager != null) {
                socketManager.disconnect(username, this, this);
            }
            PatientLoginActivity loginActivity = new PatientLoginActivity();
            Bundle bundle = new Bundle();
            bundle.putString("role", "patient");
            loginActivity.setArguments(bundle);
            ((MainActivity) getActivity()).navigateToFragment(loginActivity);
        });
        attachmentButton.setOnClickListener(this::showAttachmentOptions);
    return view;
    }
    // Method to display messages in the chat
    private void displayMessages(JSONArray messages, String username) {
        boolean hasAudioData = false;
        for (int i = 0; i < messages.length(); i++) {
            try {
                JSONObject jsonObject = messages.getJSONObject(i);

                if (jsonObject.getString("username").equals(username)) {
                    if (jsonObject.has("message")) {
                        String message = jsonObject.getString("message");
                        addMessageToChat(message, true);  // Display received message
                    }
                    if (jsonObject.has("audio_data")) {
                        String audioDataString = jsonObject.getString("audio_data");
                        convertAudioData(audioDataString);
                        hasAudioData = true;
                        addPlayButtonToChat();  // Add play button for audio message
                    }
                    if (jsonObject.has("image_base64")) {
                        String encodedImage = jsonObject.getString("image_base64");
                        addImageToView(encodedImage);  // Display image
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void showAttachmentOptions(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.attachment_options, null);
        bottomSheetDialog.setContentView(sheetView);
        ImageButton btnSelectImage = sheetView.findViewById(R.id.btn_select_image);
        ImageButton btnSelectWav = sheetView.findViewById(R.id.btn_select_wav);
        btnSelectImage.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            selectedImages.clear();
            selectImage();
        });
        btnSelectWav.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setOnDismissListener(dialog -> selectedImages.clear());
        bottomSheetDialog.show();
    }
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
        selectImageLauncher.launch(intent);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                handleImageSelected(imageUri);
                            }
                        } else if (data.getData() != null) {
                            Uri imageUri = data.getData();
                            handleImageSelected(imageUri);
                        }
                    }
                }
        );
    }

    private void handleImageSelected(Uri imageUri) {
        selectedImages.add(imageUri);
        showSelectedImagesPopup();
    }

    private void showSelectedImagesPopup() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.popup_selected_images);
        dialog.setCanceledOnTouchOutside(false);
        LinearLayout selectedImagesContainer = dialog.findViewById(R.id.selectedImagesContainer);
        Button sendButton = dialog.findViewById(R.id.sendButton);
        selectedImagesContainer.removeAllViews();
        // Add selected images to the container
        for (Uri imageUri : selectedImages) {
            ImageView imageView = new ImageView(requireContext());
            imageView.setImageURI(imageUri);
            // Scale the images to fit better
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300 // Set height to a smaller size
            ));
            imageView.setAdjustViewBounds(true);
            selectedImagesContainer.addView(imageView);
        }
        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            sendSelectedImages();
            dialog.dismiss();
        });

        dialog.show();
    }
    private void sendSelectedImages() {
        try {
            if(trustManagers != null){
                trustManagers = createTrustManagers();
            }
            socketManager.createSocket(username, trustManagers, this, this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImageToServer(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            byte[] imageData = getBytes(inputStream);
            String encodedImage = Base64.encodeToString(imageData, Base64.DEFAULT);
            socketManager.sendMessage(username, "sendImage", null, null, null, null,encodedImage);
            addImageToView(encodedImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void sendMessage() {
         pendingMessage  = messageEditText.getText().toString();
        if (pendingMessage != null && !pendingMessage.isEmpty()) {
            addMessageToChat(pendingMessage , true);
            try {
                if(trustManagers != null){
                    trustManagers = createTrustManagers();
                }
            socketManager.createSocket(username,trustManagers, this, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    @Override
    public void onConnected() {
        if (pendingMessage != null) {
        socketManager.sendMessage(username, "sendMessage", null, null, pendingMessage, null, null);
        messageEditText.setText("");
        pendingMessage = null;
        }
        if (pendingAudioCsv != null) {
            socketManager.sendMessage(username, "sendAudio", null, null, null, pendingAudioCsv, null);
            pendingAudioCsv = null;
        }
        if(selectedImages != null &&!selectedImages.isEmpty()){
        for (Uri imageUri : selectedImages) {
            sendImageToServer(imageUri);
        }
        selectedImages.clear();
        }
    }
    @Override
    public void onDisconnected() {
        socketManager.sendMessage(username, "disconnect", null, null, null, null, null);
    }

    @Override
    public void onConnectionFailed(Exception e) {}

    private void addMessageToChat(String message, boolean isUserMessage) {
        TextView textView = new TextView(requireContext());
        textView.setText(message);
        textView.setTextSize(16);
        textView.setPadding(16, 8, 16, 8);
        textView.setTextColor(getResources().getColor(R.color.textColorPrimary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (isUserMessage) {
            textView.setBackgroundResource(R.drawable.user_message_background);
            params.gravity = Gravity.START;
        } else {
            textView.setBackgroundResource(R.drawable.server_message_background);
            params.gravity = Gravity.END;
        }
        textView.setLayoutParams(params);
        messageArea.addView(textView);
    }

    private void showRecordPopup() {
        recordDialog = new Dialog(requireContext());
        recordDialog.setContentView(R.layout.record_popup);
        recordDialog.setTitle("Record Audio");

        TextView recordingStatus = recordDialog.findViewById(R.id.recordingStatus);
        ProgressBar recordingProgress = recordDialog.findViewById(R.id.recordingProgress);
        Button recordButton = recordDialog.findViewById(R.id.recordButton);
        Button playButton = recordDialog.findViewById(R.id.playButton);

        Button uploadButton = recordDialog.findViewById(R.id.uploadButton);

        recordButton.setOnClickListener(v -> {
            startRecording();
            dialogUIManagement(recordingStatus,recordingProgress, recordButton,playButton,
                    uploadButton);
        });
        playButton.setOnClickListener(v -> playRecording());

        uploadButton.setOnClickListener(v -> {
            sendAudioToServer();
            addPlayButtonToChat();
            recordDialog.dismiss();
        });
        recordDialog.show();
    }

    private void addPlayButtonToChat() {
        LinearLayout buttonLayout = new LinearLayout(requireContext());
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.START);
        int heightInPixels = (int) (48 * getResources().getDisplayMetrics().density + 0.5f);
        Button playButton = new Button(requireContext());
        playButton.setText("Play Audio");
        LinearLayout.LayoutParams playButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels
        );
        playButtonParams.gravity = Gravity.START; // Align button to the left
        playButton.setLayoutParams(playButtonParams);
        playButton.setOnClickListener(v -> playRecording());

        Button downloadButton = new Button(requireContext());
        downloadButton.setText("Download Audio");
        LinearLayout.LayoutParams downloadButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                heightInPixels
        );
        downloadButtonParams.gravity = Gravity.START; // Align button to the left
        downloadButton.setLayoutParams(downloadButtonParams);// Add your download icon here
        downloadButton.setOnClickListener(v -> downloadAudio());

        buttonLayout.addView(playButton);
        buttonLayout.addView(downloadButton);
        messageArea.addView(buttonLayout);
    }

    private void downloadAudio() {
        String path = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/recording_patient.wav";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            writeWaveFileHeader(fos, SAMPLE_RATE, 1, 16);
            byte[] byteData = shortToByte(audioData, audioData.length);
            fos.write(byteData, 0, byteData.length);
            updateWaveFileHeader(path, 44100, 1, 16);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            return;
        }
        audioFilePath = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/recording_auto.wav";
        bufferSize = 441000;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize*2);
        audioRecord.startRecording();
        isRecording = true;
        audioData = new short[bufferSize]; // Initialize audioData array
        recordingThread = new Thread(() -> {
            writeAudioDataToWavFile();
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToWavFile() {
        try (FileOutputStream fos = new FileOutputStream(audioFilePath)) {
            writeWaveFileHeader(fos, SAMPLE_RATE, 1, 16);
            while (isRecording) {
                int read = audioRecord.read(audioData, 0, bufferSize);
                System.out.println(read);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    byte[] byteData = shortToByte(audioData, read);
                    fos.write(byteData, 0, byteData.length);
                    System.out.println("Length og Bytedata:"+byteData.length);
                }
            }
            updateWaveFileHeader(audioFilePath, SAMPLE_RATE, 1, 16);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] shortToByte(short[] data, int length) {
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            bytes[i * 2] = (byte) (data[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (data[i] >> 8);
        }
        return bytes;
    }

    private void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
        }
    }

    private void playRecording() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSizeInBytes = bufferSize * 2; // Since each sample is 2 bytes (16-bit PCM)

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSizeInBytes,
                AudioTrack.MODE_STATIC
        );

        audioTrack.write(audioData, 0, bufferSize);
        audioTrack.play();
    }

    public String byteArray_to_csvString(short [ ] A)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<A.length; i++)
        {
            sb=sb.append(A[i]);
            if(i != A.length-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }


    private void sendAudioToServer() {
        String csvS = byteArray_to_csvString(audioData); // Convert audio data to CSV string
        pendingAudioCsv = "UPLOAD " + csvS + "\n"; // Prefix with "UPLOAD "

        try {
            if(trustManagers != null){
                trustManagers = createTrustManagers();
            }
            socketManager.createSocket(username, trustManagers, this, this);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to create TrustManager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void handleServerResponse(String response) {

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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void dialogUIManagement(TextView recordingStatus, ProgressBar recordingProgress, Button recordButton, Button playButton, Button uploadButton) {
        recordingStatus.setText("Recording...");
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(recordingProgress, "progress", 0, 100);
        progressAnimator.setDuration(10000); // 10 seconds
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.start();

        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recordingStatus.setText("Ready to record");
                recordingProgress.setProgress(0);
                recordButton.setEnabled(true);
                playButton.setEnabled(true);
                uploadButton.setEnabled(true);
            }
        });
        recordButton.setEnabled(false);
        playButton.setEnabled(false);
        uploadButton.setEnabled(false);

        Handler handler = new Handler();
        for (int i = 0; i <= 10; i++) {
            final int progress = i;
            handler.postDelayed(() -> recordingProgress.setProgress(progress), i * 1000);
        }

        handler.postDelayed(() -> {
            if (isRecording) {
                stopRecording();
                recordingStatus.setText("Recording stopped");
                recordButton.setEnabled(true);
                playButton.setEnabled(true);
                uploadButton.setEnabled(true);
                recordingProgress.setProgress(0);
            }
        }, 10000); // Stop recording after 10 seconds
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
            File imageFile = new File(downloadDir, "image_" + System.currentTimeMillis() + ".png");
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
}
