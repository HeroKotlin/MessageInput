package com.github.herokotlin.messageinput

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.cjt2325.cameralibrary.JCameraView
import com.cjt2325.cameralibrary.listener.ErrorListener
import com.cjt2325.cameralibrary.listener.JCameraListener
import com.cjt2325.cameralibrary.util.FileUtil
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.IOException

internal class CameraActivity : AppCompatActivity() {

    companion object {
        const val RESULT_CODE_VIDEO = 101
        const val RESULT_CODE_PHOTO = 102
        const val RESULT_CODE_ERROR = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        var flags = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        window.decorView.systemUiVisibility = flags


        setContentView(R.layout.activity_camera)

        cameraView.setFeatures(JCameraView.BUTTON_STATE_BOTH)

        cameraView.setSaveVideoPath(externalCacheDir.absolutePath)

        cameraView.setTip(resources.getString(R.string.message_input_camera_tip))

        cameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_LOW)
        cameraView.setErrorLisenter(object: ErrorListener {
            override fun AudioPermissionError() {
            }

            override fun onError() {
                setResult(RESULT_CODE_ERROR)
                finish()
            }
        })

        cameraView.setJCameraLisenter(object: JCameraListener {
            override fun recordSuccess(url: String?, firstFrame: Bitmap?) {

                val intent = Intent()
                intent.putExtra("video", url)
                intent.putExtra("thumbnail", FileUtil.saveBitmap("photo", firstFrame))

                val player = MediaPlayer()
                try {
                    player.setDataSource(url)
                    player.prepare()
                    intent.putExtra("duration", player.duration)
                    setResult(RESULT_CODE_VIDEO, intent)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                    setResult(RESULT_CODE_ERROR)
                }

                finish()
            }

            override fun captureSuccess(bitmap: Bitmap?) {
                val intent = Intent()
                intent.putExtra("photo", FileUtil.saveBitmap("photo", bitmap))
                setResult(RESULT_CODE_PHOTO, intent)
                finish()
            }
        })

        cameraView.setLeftClickListener {
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        cameraView.onResume()
    }

    override fun onPause() {
        super.onPause()
        cameraView.onPause()
    }

}
