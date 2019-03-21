package com.github.herokotlin.messageinput

import android.app.Activity
import android.support.v4.app.ActivityCompat
import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.ImageFile

interface MessageInputCallback {

    fun onRequestPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun onRecordAudioDurationLessThanMinDuration() {

    }

    fun onRecordAudioExternalStorageNotWritable() {

    }

    fun onRecordAudioPermissionsNotGranted() {

    }

    fun onRecordAudioPermissionsGranted() {

    }

    fun onRecordAudioPermissionsDenied() {

    }



    fun onRecordVideoExternalStorageNotWritable() {

    }

    fun onRecordVideoPermissionsNotGranted() {

    }

    fun onRecordVideoPermissionsGranted() {

    }

    fun onRecordVideoPermissionsDenied() {

    }

    fun onUseAudio() {

    }

    fun onSendAudio(audioPath: String, audioDuration: Int) {

    }

    fun onSendVideo(videoPath: String, videoDuration: Int, thumbnail: ImageFile) {

    }

    fun onSendPhoto(photo: ImageFile) {

    }

    fun onSendText(text: String) {

    }

    fun onSendEmotion(emotion: Emotion) {

    }

    fun onTextChange(text: String) {

    }

    fun onClickPhotoFeature() {

    }

    fun onClickFileFeature() {

    }

    fun onClickUserFeature() {

    }

    fun onClickMovieFeature() {

    }

    fun onClickPhoneFeature() {

    }

    fun onClickLocationFeature() {

    }

    fun onLift() {

    }

    fun onFall() {

    }
}