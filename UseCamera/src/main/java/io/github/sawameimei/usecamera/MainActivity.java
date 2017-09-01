package io.github.sawameimei.usecamera;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 3. 在 Android 平台使用 Camera API 进行视频的采集，分别使用 SurfaceView、TextureView 来预览 Camera 数据，取到 NV21 的数据回调
 */
public class MainActivity extends AppCompatActivity {

    private Camera fontCamera;
    private Camera backCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        TextureView ç = (TextureView) findViewById(R.id.textureView);
        final SurfaceView Ω = (SurfaceView) findViewById(R.id.surfaceView);
        TextView œ = (TextView) findViewById(R.id.capturing);

        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera) {
            Toast.makeText(this, "没有摄像头！！", Toast.LENGTH_SHORT).show();
            return;
        }
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras < 0) {
            return;
        }
        fontCamera = Camera.open();
        if (numberOfCameras > 1) {
            backCamera = Camera.open(1);
        } else {
            backCamera = fontCamera;
        }

        /*œ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fontCamera!=null){
                    fontCamera.stopPreview();
                    fontCamera.release();
                }

                if(backCamera!=null){
                    backCamera.stopPreview();
                    backCamera.release();
                }
                startActivity(new Intent(getApplication(), CaptureVideoActivity.class));
            }
        });*/

        /**
         * https://developer.android.google.cn/guide/topics/media/camera.html#considerations
         */
        SurfaceHolder holder = Ω.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    fontCamera.setPreviewDisplay(holder);
                    fontCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (holder.getSurface() == null) {
                    return;
                }
                try {
                    fontCamera.stopPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    fontCamera.setPreviewDisplay(holder);
                    fontCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                fontCamera.stopPreview();
                fontCamera.release();
            }
        });

        /**
         * http://web.mit.edu/majapw/MacData/afs/sipb/project/android/docs/reference/android/view/TextureView.html
         */
        ç.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    backCamera.setPreviewTexture(surface);
                    backCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                try {
                    backCamera.stopPreview();
                } catch (Exception e) {

                }
                try {
                    backCamera.setPreviewTexture(surface);
                    backCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                backCamera.stopPreview();
                backCamera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                try {
                    backCamera.stopPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    backCamera.setPreviewTexture(surface);
                    backCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
