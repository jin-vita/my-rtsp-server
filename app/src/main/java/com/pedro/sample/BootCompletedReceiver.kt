package com.pedro.sample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.SyncStateContract
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.*
import java.util.*

/**
 * 단말 재시작 시 메시지를 수신하는 수신자
 */
class BootCompletedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        println("onReceive called in BootCompletedReceiver")

        // 대상 화면 띄우기
        context?.apply {

            /*
            Handler().postDelayed(Runnable {
                startServer(this)
            }, 20000)
            */

            /*
            val runtime = Runtime.getRuntime();
            val intentCommand =  "su -c am start -n com.pedro.sample/.MainActivity -a android.intent.action.VIEW";
            Log.i("TAG", intentCommand);
            runtime.exec(intentCommand);
            */

            //Handler().postDelayed(Runnable {
            //    startActivity(this)
            //}, 5000)


            // 화면 띄우기
            val loginIntent = Intent(this, Camera2DemoActivity::class.java)
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_SINGLE_TOP + Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(loginIntent)

        }

    }

    fun startActivity(context:Context) {

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val CHANNEL_ID = "my_notification_id"
            val CHANNEL_NAME = "my_notification_name"

            var mChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (mChannel == null) {
                mChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(mChannel);
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)

            builder.setSmallIcon(R.drawable.snow_flakes)
                .setContentTitle("Camera")
                .setContentText("Camera Server")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(openScreen(context, 1001), true)
                .setAutoCancel(true)
                .setOngoing(true);

            val notification = builder.build()
            notificationManager.notify(1001, notification);
        } else {
        */
            val startIntent = Intent(context, Camera2DemoActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(startIntent);
        //}

    }

    fun openScreen(context:Context, notificationId:Int):PendingIntent {
        val fullScreenIntent = Intent(context, Camera2DemoActivity::class.java)
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fullScreenIntent.putExtra("notificationIds", notificationId)

        return PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }





    companion object {
        val TAG = "BootReceiver"
    }

    var cameraIds = arrayOfNulls<String>(0)

    fun startServer(context:Context) {
        Log.d(TAG, "startServer called.")

        // 카메라 id 확인
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraIds = cameraManager.cameraIdList
        Log.d(TAG, "카메라 갯수 : ${cameraIds.size} ")
        cameraIds.forEachIndexed { index, cameraId ->
            cameraId?.apply {
                Log.d(TAG, "카메라 ${index} : ${this} ")

                val resolution = getResolution(cameraManager, this)
                Log.d(TAG, "(${resolution.width}, ${resolution.height})")
            }
        }

        if (cameraIds.isNotEmpty()) {
            // 첫번째 서비스 실행
            val serviceIntent = Intent(context, CameraRtspService::class.java)
            serviceIntent.putExtra("command", "start")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // 두번째 서비스 실행
            if (cameraIds.size > 1) {
                val serviceIntent2 = Intent(context, CameraRtspService2::class.java)
                serviceIntent2.putExtra("command", "start")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent2)
                } else {
                    context.startService(serviceIntent2)
                }
            }
        }

    }

    @Throws(CameraAccessException::class)
    fun getResolution(cameraManager: CameraManager, cameraId: String): Size {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw IllegalStateException("Failed to get configuration map.")
        val choices: Array<Size> = map.getOutputSizes(ImageFormat.JPEG)
        Arrays.sort(
            choices,
            Collections.reverseOrder { lhs, rhs -> // Cast to ensure the multiplications won't overflow
                java.lang.Long.signum((lhs.width * lhs.height).toLong() - (rhs.width * rhs.height).toLong())
            })

        return choices[0]
    }

    fun stopServer(context:Context) {
        // 서비스 실행
        val serviceIntent = Intent(context, CameraRtspService::class.java)
        serviceIntent.putExtra("command", "start")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }


}