package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.ImageFile

interface MessageInputCallback {

    fun onAudioSend(audioPath: String, audioDuration: Int) {

    }

    fun onVideoSend(videoPath: String, videoDuration: Int, thumbnail: ImageFile) {

    }

    fun onPhotoSend(photo: ImageFile) {

    }

    fun onTextSend(text: String) {

    }

    fun onEmotionSend(emotion: Emotion) {

    }

    fun onPhotoFeatureClick() {

    }

    fun onLift() {

    }

    fun onFall() {

    }
}