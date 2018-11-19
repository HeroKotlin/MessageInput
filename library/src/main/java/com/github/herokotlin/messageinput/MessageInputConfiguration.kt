package com.github.herokotlin.messageinput

import android.content.Context
import android.widget.ImageView

abstract class MessageInputConfiguration(val context: Context) {

    /**
     * 加载图片
     */
    abstract fun loadImage(imageView: ImageView, url: String)

}