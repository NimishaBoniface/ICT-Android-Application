package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MessagingActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        Intent messagingActivityintentintent = getIntent();
        username = messagingActivityintentintent.getStringExtra("username");

        // Initialize UI components
        messageEditText = findViewById(R.id.messageEditText);
        attachmentButton = findViewById(R.id.attachmentButton);
        sendButton = findViewById(R.id.sendButton);
        voiceMessageButton = findViewById(R.id.voiceMessageButton);
        messageArea = findViewById(R.id.messageArea);
        backButton = findViewById(R.id.backButton);

        // Set up button listeners
        sendButton.setOnClickListener(v -> sendMessage());
        voiceMessageButton.setOnClickListener(v -> showRecordPopup());
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(MessagingActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    public void showAttachmentOptions(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.attachment_options, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageButton btnSelectImage = sheetView.findViewById(R.id.btn_select_image);
        ImageButton btnSelectWav = sheetView.findViewById(R.id.btn_select_wav);

        btnSelectImage.setOnClickListener(v -> bottomSheetDialog.dismiss());
        btnSelectWav.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString();
        if (!message.isEmpty()) {
            addMessageToChat(message, true);
            messageEditText.setText("");
            new SocketClientTask(this).sendMessageToServer(message, username);
        }
    }

    private void addMessageToChat(String message, boolean isUserMessage) {
        TextView textView = new TextView(this);
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
        recordDialog = new Dialog(this);
        recordDialog.setContentView(R.layout.record_popup);
        recordDialog.setTitle("Record Audio");

        Button startRecordingButton = recordDialog.findViewById(R.id.startRecordingButton);
        Button stopRecordingButton = recordDialog.findViewById(R.id.stopRecordingButton);
        Button playRecordingButton = recordDialog.findViewById(R.id.playRecordingButton);
        Button uploadButton = recordDialog.findViewById(R.id.uploadButton);

        startRecordingButton.setOnClickListener(v -> {
            startRecording();
            startRecordingButton.setVisibility(View.GONE);
            stopRecordingButton.setVisibility(View.VISIBLE);
        });

        stopRecordingButton.setOnClickListener(v -> {
            stopRecording();
            stopRecordingButton.setVisibility(View.GONE);
            playRecordingButton.setVisibility(View.VISIBLE);
            uploadButton.setVisibility(View.VISIBLE);
        });

        playRecordingButton.setOnClickListener(v -> playRecording());

        uploadButton.setOnClickListener(v -> {
            sendAudioToServer();
            recordDialog.dismiss();
        });

        recordDialog.show();
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            return;
        }
        audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/recording.wav";
//        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
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
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String senMsg = "UPLOAD " + csvS + "\n"; // Prefix with "UPLOAD "
        new SocketClientTask(this).sendAudioToServer(senMsg, username);
    }

    public void handleServerResponse(String response) {
        addMessageToChat(response, false);
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
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
