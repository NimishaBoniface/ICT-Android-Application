package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.View;

import java.io.File;

import android.widget.EditText;
import android.media.AudioManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.view.Gravity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.app.Dialog;
import android.widget.Button;

public class MessagingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2000;
    private static final int REQUEST_CODE_SELECT_WAV = 3000;

    private static final int REQUEST_CODE_RECORD_AUDIO = 1001;


    private EditText messageEditText;
    private ImageButton attachmentButton, sendButton, voiceMessageButton, speechToTextButton;
    private static final int SAMPLE_RATE = 44100;

    private LinearLayout messageArea;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private Dialog recordDialog;
    private boolean isRecording = false;
    private WavRecorder wavRecorder;
    private AudioRecord audioRecord;
    private Thread recordingThread;

    private int bufferSize;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);


        messageEditText = findViewById(R.id.messageEditText);
        attachmentButton = findViewById(R.id.attachmentButton);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setEnabled(true);
        voiceMessageButton = findViewById(R.id.voiceMessageButton);
        speechToTextButton = findViewById(R.id.speechToTextButton);
        messageArea = findViewById(R.id.messageArea);

        // Set up button listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        voiceMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecordPopup();
            }
        });
        speechToTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        // Other setup code for loading messages, setting up voice messages, attachments, etc.
    }


    public void showAttachmentOptions(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.attachment_options, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageButton btnSelectImage = sheetView.findViewById(R.id.btn_select_image);
        ImageButton btnSelectWav = sheetView.findViewById(R.id.btn_select_wav);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
                bottomSheetDialog.dismiss();
            }
        });

        btnSelectWav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWav();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    private void selectWav() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/wav");
        startActivityForResult(intent, REQUEST_CODE_SELECT_WAV);
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString();
        if (!message.isEmpty()) {
            // Code to send message
            addMessageToChat(message, true);

            // Clear the input field
            messageEditText.setText("");

            new SocketClientTask(this).sendMessageToServer(message);
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

        // Set background based on message type
        if (isUserMessage) {
            textView.setBackgroundResource(R.drawable.user_message_background);
            params.gravity = Gravity.START;
        } else {
            textView.setBackgroundResource(R.drawable.server_message_background);
            params.gravity = Gravity.END;
        }

        // Add TextView to message area
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

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    startRecording();
                    startRecordingButton.setVisibility(View.GONE);
                    stopRecordingButton.setVisibility(View.VISIBLE);
                }
            }
        });

        stopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                stopRecordingButton.setVisibility(View.GONE);
                playRecordingButton.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.VISIBLE);
            }
        });

        playRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecording();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAudioToServer();
                recordDialog.dismiss();
            }
        });

        recordDialog.show();
    }


    private void startRecording() {
        if (isExternalStorageWritable() && checkStorageSpace()) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO},REQUEST_CODE_RECORD_AUDIO);
            }
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/recording.raw";
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile(bufferSize);
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }
    }

    private void writeAudioDataToFile(int bufferSize) {
        byte data[] = new byte[bufferSize];
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(audioFilePath);
            while (isRecording) {
                int read = audioRecord.read(data, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    fos.write(data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
            copyWaveFile(audioFilePath, getWavFilePath());
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getWavFilePath() {
        return getExternalCacheDir().getAbsolutePath() + "/recording2.wav";
    }

    private void writeWaveFileHeader(
            FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];

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
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void playRecording() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getWavFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.READ_MEDIA_AUDIO
                }, REQUEST_CODE_RECORD_AUDIO);

                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_CODE_RECORD_AUDIO);

                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void sendAudioToServer() {
        // Implement your logic to convert the audio to CSV format and send to server
    }

    public void handleServerResponse(String response) {
        // Display server response
        addMessageToChat(response, false);
    }



    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            // Handle exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                messageEditText.setText(result.get(0));
            }
        }
    }
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private boolean checkStorageSpace() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir != null) {
            long freeSpace = storageDir.getFreeSpace();
            long requiredSpace = bufferSize * 60 * 5; // Assuming 5 minutes of recording
            return freeSpace > requiredSpace;
        }
        return false;
    }
}
