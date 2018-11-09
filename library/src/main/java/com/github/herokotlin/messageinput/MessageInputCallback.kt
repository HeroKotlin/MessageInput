package com.github.herokotlin.messageinput

import com.github.herokotlin.emotioninput.model.Emotion
import com.github.herokotlin.messageinput.model.Image

interface MessageInputCallback {

    fun onAudioRecordSuccess(file: String, duration: Int) {

    }

    fun onVideoRecordSuccess(file: String, duration: Int, firstFrame: Image) {

    }

    fun onPhotoCaptureSuccess(photo: Image) {

    }

    fun onMediaSelectSuccess(files: List<Image>) {

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