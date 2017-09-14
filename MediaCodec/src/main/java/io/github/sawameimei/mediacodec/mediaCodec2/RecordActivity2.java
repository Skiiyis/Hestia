package io.github.sawameimei.mediacodec.mediaCodec2;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

import io.github.sawameimei.mediacodec.R;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by huangmeng on 2017/9/13.
 */

/**
 * 从surfaceView上拿取数据再编码，编码帧率和camera预览帧率一致
 */
public class RecordActivity2 extends AppCompatActivity implements View.OnClickListener {

    private GLSurfaceView glSurfaceview;
    private Button record;
    private Button play;
    private SurfaceTexture surfaceTexture;

    private File recordingFile;
    private boolean recording;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_2);
        //glSurfaceview = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        record = (Button) findViewById(R.id.record);
        play = (Button) findViewById(R.id.play);
        record.setOnClickListener(this);
        play.setOnClickListener(this);
        initPreview();
    }

    private void initPreview() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio/");
        file.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".mp4", file);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:

                break;
            case R.id.record:
                record.setText(recording ? "停止录制" : "点我录制");
                break;
        }
    }
}
