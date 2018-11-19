package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.Image

interface MessageInputCallback {

    fun onAudioSend(audioPath: String, audioDuration: Int) {

    }

    fun onVideoSend(videoPath: String, videoDuration: Int, thumbnail: Image) {

    }

    fun onPhotoSend(photo: Image) {

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