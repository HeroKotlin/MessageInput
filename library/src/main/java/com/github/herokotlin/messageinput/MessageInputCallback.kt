package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.Image

interface MessageInputCallback {

    fun onVoiceSend(audioPath: String, audioDuration: Int) {

    }

    fun onVideoSend(videoPath: String, videoDuration: Int, photoPath: String, photoWidth: Int, photoHeight: Int) {

    }

    fun onPhotoSend(photoPath: String, photoWidth: Int, photoHeight: Int) {

    }

    fun onImageSend(images: List<Image>) {

    }

    fun onTextSend(text: String) {

    }

    fun onEmotionSend(emotion: Emotion) {

    }

    fun onLift() {

    }

    fun onFall() {

    }
}