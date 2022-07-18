package com.setik.androidutils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.unity3d.player.UnityPlayer;

public class RequestScreenRecordActivity extends Activity {
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    public static void Launch(Activity activity)
    {
        // Creating an intent with the current activity and the activity we wish to start
        Intent myIntent = new Intent(activity, RequestScreenRecordActivity.class);
        activity.startActivity(myIntent);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Window window = getWindow();
        window.setDimAmount(0); //Making the window dim transparent
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) UnityPlayer.currentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //Start screen recording
                UnityPlayer.UnitySendMessage("ScreenRecorder", "SuccessCallback", "screen_record_accepted");
                ScreenRecorder.instance.startRecording_internal(resultCode, data);
            }
            else {
                UnityPlayer.UnitySendMessage("ScreenRecorder", "ErrorCallback", "screen_record_denied");
            }
        }
        finish();
    }
}
