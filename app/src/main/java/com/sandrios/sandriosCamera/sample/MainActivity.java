package com.sandrios.sandriosCamera.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sandrios.sandriosCamera.internal.SandriosCamera;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.manager.CameraOutputModel;

import java.util.ArrayList;

/**
 * Sample for Sandrios Camera library
 * Created by Arpit Gandhi on 11/8/16.
 */

public class MainActivity extends AppCompatActivity {

    private Activity activity;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.withPicker:
                    SandriosCamera
                            .with(activity)
                            .setShowPicker(true)
                            .setShowPickerType(CameraConfiguration.VIDEO)
                            .setVideoFileSize(20)
                            .setMediaAction(CameraConfiguration.MEDIA_ACTION_BOTH)
                            .enableImageCropping(true)
                            .launchCamera(new SandriosCamera.CameraCallback() {
                                @Override
                                public void onComplete(CameraOutputModel model) {
                                    Log.e("File", "" + model.getPath());
                                    Log.e("Type", "" + model.getType());
                                    Toast.makeText(getApplicationContext(), "Media captured.", Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
                case R.id.withoutPicker:
                    SandriosCamera
                            .with(activity)
                            .setShowPicker(false)
                            .setMediaAction(CameraConfiguration.MEDIA_ACTION_PHOTO)
                            .enableImageCropping(false)
                            .launchCamera(new SandriosCamera.CameraCallback() {
                                @Override
                                public void onComplete(CameraOutputModel model) {
                                    Log.e("File", "" + model.getPath());
                                    Log.e("Type", "" + model.getType());
                                    Toast.makeText(getApplicationContext(), "Media captured.", Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);
        activity = this;

        findViewById(R.id.withPicker).setOnClickListener(onClickListener);
        findViewById(R.id.withoutPicker).setOnClickListener(onClickListener);
    }
}
