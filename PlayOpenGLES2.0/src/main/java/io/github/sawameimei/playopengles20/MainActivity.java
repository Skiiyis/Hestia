package io.github.sawameimei.playopengles20;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PackageInfo packageInfo;
    private ActivityInfo[] activities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        listView.setDivider(null);
        setContentView(listView);

        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        activities = packageInfo.activities;
        ArrayList<ActivityInfo> al = new ArrayList<>();
        for (ActivityInfo activity : activities) {
            if (!activity.name.endsWith("MainActivity")) {
                al.add(activity);
            }
        }
        activities = al.toArray(new ActivityInfo[]{});
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return activities.length;
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
                Button button = new Button(MainActivity.this);
                ActivityInfo activityInfo = activities[position];
                final String activityInfoName = activityInfo.name;
                CharSequence label = activityInfo.loadLabel(getPackageManager());
                button.setText(label);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchActivity(MainActivity.this, activityInfoName);
                    }
                });
                return button;
            }
        });
    }

    public static void launchActivity(Activity activity, String activityName) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(activity.getPackageName(), activityName));
        if (activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            Toast.makeText(activity, "找不到目标Activity", Toast.LENGTH_SHORT).show();
        } else {
            activity.startActivity(intent);
        }
    }
}
