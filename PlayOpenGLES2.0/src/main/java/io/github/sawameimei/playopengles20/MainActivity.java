package io.github.sawameimei.playopengles20;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        listView.setDivider(null);
        setContentView(listView);
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 6;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                position++;
                Button button = new Button(MainActivity.this);
                final String activityName = "OpenGLES20L" + position + "Activity";
                button.setText(activityName);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchActivity(MainActivity.this, activityName);
                    }
                });
                return button;
            }
        });
    }

    public static void launchActivity(Activity activity, String activityName) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(activity.getPackageName(), activity.getPackageName() + "." + activityName));
        if (activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            Toast.makeText(activity, "找不到目标Activity", Toast.LENGTH_SHORT).show();
        } else {
            activity.startActivity(intent);
        }
    }
}
