package com.github.herokotlin.messageinput

import android.content.Context
import android.widget.ImageView
import com.github.herokotlin.messageinput.enum.FeatureType

abstract class MessageInputConfiguration(val context: Context) {

    var featureList = listOf(
        FeatureType.PHOTO,
        FeatureType.CAMERA,
        FeatureType.FILE,
        FeatureType.USER,
        FeatureType.MOVIE,
        FeatureType.PHONE,
        FeatureType.LOCATION,
        FeatureType.FAVOR
    )

    /**
     * 表情和文本的高度比例
     */
    var emotionTextHeightRatio = 1f

    /**
     * 码率
     */
    var audioBitRate = 320000

    /**
     * 采样率
     */
    var audioSampleRate = 44100

    /**
     * 加载图片
     */
    abstract fun loadImage(imageView: ImageView, url: String)

}