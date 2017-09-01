package io.github.sawameimei.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static android.media.AudioTrack.MODE_STREAM;

/**
 * Created by huangmeng on 2017/9/1.
 */

public class PCMRecordByAudioRecorder implements AudioOperation {

    private boolean isRecoding;
    private File recordingFile;
    private final int SAMPLE_RATE = 44100;

    public PCMRecordByAudioRecorder() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".pcm", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String filePath() {
        return recordingFile.getAbsolutePath();
    }

    @Override
    public void startRecord() {
        isRecoding = true;
        new Thread() {
            @Override
            public void run() {
                int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
                try {
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                    byte[] buffer = new byte[minBufferSize];
                    audioRecord.startRecording();
                    while (isRecoding) {
                        audioRecord.read(buffer, 0, buffer.length);
                        dos.write(buffer);
                    }
                    audioRecord.stop();
                    audioRecord.release();
                    dos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void stopRecord() {
        isRecoding = false;
    }

    @Override
    public void play() {
        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        byte[] buffer = new byte[minBufferSize];
        try {
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer.length, MODE_STREAM);
            audioTrack.play();
            while (dataInputStream.available() > 0 && dataInputStream.read(buffer) > 0) {
                audioTrack.write(buffer, 0, buffer.length);
            }
            audioTrack.stop();
            dataInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
