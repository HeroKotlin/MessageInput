package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.ImageFile

interface MessageInputCallback {

    fun onChildViewChange() {

    }

    fun onRecordAudioWithoutPermissions() {

    }

    fun onRecordAudioDurationLessThanMinDuration() {

    }

    fun onRecordAudioWithoutExternalStorage() {

    }

    fun onRecordAudioPermissionsGranted() {

    }

    fun onRecordAudioPermissionsDenied() {

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

    fun onLift() {

    }

    fun onFall() {

    }
}