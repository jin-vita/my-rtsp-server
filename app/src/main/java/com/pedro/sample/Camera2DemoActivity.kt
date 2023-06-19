package com.pedro.sample

import android.Manifest
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera2
import kotlinx.android.synthetic.main.activity_camera2_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Camera2DemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener,
    SurfaceHolder.Callback {

  private val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE)

  private lateinit var rtspServerCamera2: RtspServerCamera2
  private lateinit var button: Button
  private lateinit var bRecord: Button
  private lateinit var output1: TextView

  private var currentDateAndTime = ""
  private lateinit var folder: File

  val handler = Handler()

  var cameraIds = arrayOfNulls<String>(0)



  // 화면잠금
  var deviceManger: DevicePolicyManager? = null
  var compName: ComponentName? = null
  var active = false

  var systemAlertDialogShown = false




  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_camera2_demo)

    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop)
    button.setOnClickListener(this)
    bRecord = findViewById(R.id.b_record)
    bRecord.setOnClickListener(this)
    switch_camera.setOnClickListener(this)

    output1 = findViewById(R.id.output1)


    // 프리뷰 화면을 보고싶다면...
    //rtspServerCamera2 = RtspServerCamera2(surfaceView, this, 1935)
    //surfaceView.holder.addCallback(this)


    val exposureButton = findViewById<Button>(R.id.exposureButton)
    exposureButton.setOnClickListener {
      rtspServerCamera2.exposure = rtspServerCamera2.maxExposure

      Toast.makeText(this, "max exposure : ${rtspServerCamera2.maxExposure}", Toast.LENGTH_SHORT).show()
    }

    if (!hasPermissions(this, *PERMISSIONS)) {
      ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
    } else {
      // 인텐트 처리
      intent?.apply {
        handleIntent(this)
      }
    }

  }

  private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
      for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(context,
            permission) != PackageManager.PERMISSION_GRANTED) {
          return false
        }
      }
    }
    return true
  }

  override fun onNewIntent(intent: Intent?) {

    intent?.apply {
      handleIntent(this)
    }

    super.onNewIntent(intent)
  }

  fun handleIntent(intent: Intent?) {
    if (intent != null) {
      // 서버 시작
      startServer()

      // 5초 후에 화면 종료
      handler.postDelayed(
        { moveTaskToBack(true) },
        5000)

    }
  }


  override fun onResume() {
    super.onResume()

    // 위험권한 부여 요청
    requestPermission()

  }

  fun requestPermission() {
    // 단말 관리자 앱으로 설정 요청
    deviceManger = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    compName = ComponentName(this, DeviceAdmin::class.java)
    active = deviceManger!!.isAdminActive(compName!!)
    Log.d("Camera", "deviceAdmin is active : ${active}")

    if (!active) {
      val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "기기 관리자 앱으로 권한을 부여합니다")
      }
      startActivityForResult(intent, 1001)
    } else {
      showSystemAlertWindowPermission()
    }
  }


  /**
   * 다른 앱 위에 그리기 권한 허용 여부 확인
   */
  fun showSystemAlertWindowPermission(): Boolean {
    Log.d("Camera", "showSystemAlertWindowPermission() called.")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!Settings.canDrawOverlays(this)) {
        val dialog = AlertDialog.Builder(this)
        dialog.setCancelable(false)
        dialog.setTitle("다른 앱 위에 그리기 권한을 부여하셔야 사용하실 수 있습니다.")
        dialog.setPositiveButton("확인") { dialog, which ->
          dialog.dismiss()

          val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(
              "package:$packageName"
            )
          )
          startActivity(intent)

        }
        dialog.setNegativeButton("무시") { dialog, which ->
          dialog.dismiss()
        }

        dialog.show()
        systemAlertDialogShown = true

      } else {
        // TODO : 셋톱박스 제외해야 함
        //checkLocation()
      }
    }

    return true
  }



  fun startServer() {

    // 카메라 id 확인
    val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
    cameraIds = cameraManager.cameraIdList
    output1.append("카메라 갯수 : ${cameraIds.size} ")
    cameraIds.forEachIndexed { index, cameraId ->
      cameraId?.apply {
        output1.append("카메라 ${index} : ${this} ")

        val resolution = getResolution(cameraManager, this)
        output1.append("(${resolution.width}, ${resolution.height})")
      }
    }

    if (cameraIds.isNotEmpty()) {
      // 첫번째 서비스 실행
      val serviceIntent = Intent(applicationContext, CameraRtspService::class.java)
      serviceIntent.putExtra("command", "start")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
      } else {
        startService(serviceIntent)
      }

      // 두번째 서비스 실행
      if (cameraIds.size > 1) {
        val serviceIntent2 = Intent(applicationContext, CameraRtspService2::class.java)
        serviceIntent2.putExtra("command", "start")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          startForegroundService(serviceIntent2)
        } else {
          startService(serviceIntent2)
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

  fun stopServer() {
    // 서비스 실행
    val serviceIntent = Intent(applicationContext, CameraRtspService::class.java)
    serviceIntent.putExtra("command", "start")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }
  }

  /**
   * 시작하거나 중지하기
   */
  private fun startStopServer() {

    if (!rtspServerCamera2.isStreaming) {

      val videoWidth = 1920
      val videoHeight = 1080
      val fps = 30
      val bitrate = 1200 * 1024
      val rotation = CameraHelper.getCameraOrientation(this)

      if (rtspServerCamera2.isRecording || rtspServerCamera2.prepareAudio() &&
        //rtspServerCamera2.prepareVideo(videoWidth, videoHeight, fps, bitrate, rotation)) {
        rtspServerCamera2.prepareVideo()) {

        button.setText(R.string.stop_button)
        rtspServerCamera2.startStream()
        tv_url.text = rtspServerCamera2.getEndPointConnection()

      } else {
        Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT).show()
      }
    } else {
      button.setText(R.string.start_button)
      rtspServerCamera2.stopStream()
      tv_url.text = ""
    }

  }

  override fun onNewBitrateRtsp(bitrate: Long) {

  }

  override fun onConnectionSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@Camera2DemoActivity, "Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtsp(reason: String) {
    runOnUiThread {
      Toast.makeText(this@Camera2DemoActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
    }
  }

  override fun onConnectionStartedRtsp(rtspUrl: String) {
  }

  override fun onDisconnectRtsp() {
    runOnUiThread {
      Toast.makeText(this@Camera2DemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAuthErrorRtsp() {
    runOnUiThread {
      Toast.makeText(this@Camera2DemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
      rtspServerCamera2.stopStream()
      button.setText(R.string.start_button)
      tv_url.text = ""
    }
  }

  override fun onAuthSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@Camera2DemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.b_start_stop -> startStopServer()

      R.id.switch_camera -> try {
          rtspServerCamera2.switchCamera()
        } catch (e: CameraOpenException) {
          Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

      R.id.b_record -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtspServerCamera2.isRecording) {
            try {
              if (!folder.exists()) {
                folder.mkdir()
              }
              val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
              currentDateAndTime = sdf.format(Date())
              if (!rtspServerCamera2.isStreaming) {
                if (rtspServerCamera2.prepareAudio() && rtspServerCamera2.prepareVideo()) {
                  rtspServerCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                  bRecord.setText(R.string.stop_record)
                  Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(
                    this, "Error preparing stream, This device cant do it",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              } else {
                rtspServerCamera2.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                bRecord.setText(R.string.stop_record)
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
              }
            } catch (e: IOException) {
              rtspServerCamera2.stopRecord()
              bRecord.setText(R.string.start_record)
              Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
          } else {
            rtspServerCamera2.stopRecord()
            bRecord.setText(R.string.start_record)
            Toast.makeText(
              this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
              Toast.LENGTH_SHORT
            ).show()
          }
        } else {
          Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
        }
      }
      else -> {
      }
    }
  }

  override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
  }

  override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
    //rtspServerCamera2.startPreview()

    val cameraId = rtspServerCamera2.camerasAvailable[0]
    rtspServerCamera2.startPreview(cameraId)
  }

  override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (rtspServerCamera2.isRecording) {
        rtspServerCamera2.stopRecord()
        bRecord.setText(R.string.start_record)
        Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath, Toast.LENGTH_SHORT).show()
        currentDateAndTime = ""
      }
    }
    if (rtspServerCamera2.isStreaming) {
      rtspServerCamera2.stopStream()
      button.text = resources.getString(R.string.start_button)
      tv_url.text = ""
    }
    rtspServerCamera2.stopPreview()
  }
}
