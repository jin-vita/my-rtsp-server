package com.pedro.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera2;
import com.pedro.sample.util.SharedPreferencesUtil;

import java.io.File;

public class CameraRtspService extends Service implements ConnectCheckerRtsp {
    private static final String TAG = "CameraRtspService";

    RtspServerCamera2 rtspServerCamera2;
    File folder;

    boolean started = false;

    @Override
    public void onCreate() {
        super.onCreate();

        File externalFolder = getExternalFilesDir(null);
        if (externalFolder != null) {
            folder = new File(externalFolder.getAbsolutePath() + "/rtmp-rtsp-stream-client-java");
        }

        rtspServerCamera2 = new RtspServerCamera2(this, true, this, 1935);
        String[] cameraIds = rtspServerCamera2.getCamerasAvailable();
        String cameraId = cameraIds[0];
        rtspServerCamera2.cameraManager.cameraId = cameraId;

        createNotificationChannel();

        String startedFlag = SharedPreferencesUtil.getValue(this, "started");
        //if (startedFlag != null && startedFlag.equals("true")) {
            Log.d(TAG, "start command.");

            startServer();

            SharedPreferencesUtil.setValue(this, "started", "true");

        //}

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            super.onStartCommand(intent, flags, startId);
            return START_STICKY;
        }

        handleCommand(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, MainActivity.class);

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("CameraRtsp 실행중")
                .setWhen(System.currentTimeMillis());


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("CameraRtsp");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("CameraRtsp", "CameraRtsp", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(111, notification);

    }

    public void handleCommand(Intent intent) {
        if (intent != null) {
            String command = intent.getStringExtra("command");
            if (command != null) {
                if (command.equals("start")) {  // 시작
                    Log.d(TAG, "start command.");

//                    if (!started) {
//                        startServer();
 //                   }

                    SharedPreferencesUtil.setValue(this, "started", "true");

                } else if (command.equals("stop")) {  // 종료
                    Log.d(TAG, "stop command.");

                    SharedPreferencesUtil.setValue(this, "started", "false");

                }

            }
        }
    }


    /**
     * 시작하기
     */
    public void startServer() {

        if (!rtspServerCamera2.isStreaming()) {

            int videoWidth = 720;
            int videoHeight = 1280;
            int fps = 15;
            int bitrate = 1200 * 1024;
            int rotation = CameraHelper.getCameraOrientation(this);

            // MIKE 220727
            // 해상도가 1920x1080을 지원하지 않는 경우 문제 발생함
            if (rtspServerCamera2.isRecording() ||
                    rtspServerCamera2.prepareVideo(videoWidth, videoHeight, fps, bitrate, rotation)) {
            //if (rtspServerCamera2.isRecording() || rtspServerCamera2.prepareAudio() &&
            //        rtspServerCamera2.prepareVideo()) {

                rtspServerCamera2.startStream();
                String url = rtspServerCamera2.getEndPointConnection();
                Log.d(TAG, "url : " + url + ", cameraId : " + rtspServerCamera2.cameraManager.cameraId);

            } else {
                Log.d(TAG, "Error preparing stream, This device cant do it");
            }

        } else {
            rtspServerCamera2.stopStream();
        }

        started = false;
    }


    /**
     * 시작하기
     */
    public void stopServer() {
        if (rtspServerCamera2.isStreaming()) {
            rtspServerCamera2.stopStream();
        }
    }

        @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called.");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnectionStartedRtsp(@NonNull String rtspUrl) {

    }

    @Override
    public void onConnectionSuccessRtsp() {
        Log.d(TAG, "Connection success");
    }

    @Override
    public void onConnectionFailedRtsp(@NonNull String reason) {
        Log.d(TAG, "Connection failed. " + reason);

        rtspServerCamera2.stopStream();
    }

    @Override
    public void onNewBitrateRtsp(long bitrate) {

    }

    @Override
    public void onDisconnectRtsp() {
        Log.d(TAG, "Disconnected");
    }

    @Override
    public void onAuthErrorRtsp() {
        Log.d(TAG, "Auth error");

        rtspServerCamera2.stopStream();
    }

    @Override
    public void onAuthSuccessRtsp() {
        Log.d(TAG, "Auth success");
    }
}