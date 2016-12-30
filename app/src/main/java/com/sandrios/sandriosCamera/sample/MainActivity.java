package com.sandrios.sandriosCamera.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sandrios.sandriosCamera.internal.SandriosCamera;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;

/**
 * Created by Arpit Gandhi on 11/8/16.
 */

public class MainActivity extends AppCompatActivity {

    private static final int CAPTURE_MEDIA = 368;

    private Activity activity;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.withPicker:
                    new SandriosCamera(activity, CAPTURE_MEDIA, true)
                            .launchCamera();
                    break;
                case R.id.withoutPicker:
                    new SandriosCamera(activity, CAPTURE_MEDIA, false)
                            .launchCamera();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_MEDIA && resultCode == RESULT_OK) {
            Log.e("File", "" + data.getStringExtra(CameraConfiguration.Arguments.FILE_PATH));
            Toast.makeText(this, "Media captured.", Toast.LENGTH_SHORT).show();
        }
    }
}
