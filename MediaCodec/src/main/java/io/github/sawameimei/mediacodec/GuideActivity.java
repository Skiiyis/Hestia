package io.github.sawameimei.mediacodec;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import io.github.sawameimei.mediacodec.mediaCodec.RecordActivity;
import io.github.sawameimei.mediacodec.mediaCodec2.RecordActivity2;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        findViewById(R.id.recordActivity)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getApplication(), RecordActivity.class));
                    }
                });
        findViewById(R.id.recordActivity2)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getApplication(), RecordActivity2.class));
                    }
                });
    }
}
