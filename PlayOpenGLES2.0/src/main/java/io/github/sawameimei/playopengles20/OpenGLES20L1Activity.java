package io.github.sawameimei.playopengles20;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class OpenGLES20L1Activity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private GLSurfaceView mGLSurfaceView;
    private LessonOneRenderer mRenderer;
    private TextView tv1;
    private TextView tv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.open_gl_es_activity);

        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        SeekBar s1 = findViewById(R.id.s1);
        SeekBar s2 = findViewById(R.id.s2);
        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        s1.setOnSeekBarChangeListener(this);
        s2.setOnSeekBarChangeListener(this);
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);
            // Set the renderer to our demo renderer, defined below.
            Log.e("error", "onCreate");
            mRenderer = new LessonOneRenderer(this);
            mGLSurfaceView.setRenderer(mRenderer);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }
    }

    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.s1) {
            float near = progress / 10F;
            mRenderer.setNear(near);
            tv1.setText("near:"+near);
        } else {
            float far = progress / 10F;
            mRenderer.setFar(far);
            tv2.setText("far:"+far);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
